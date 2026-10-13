package ru.support.adminpanel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Раздача собранного фронтенда самим backend — после отказа от nginx.
 *
 * Раньше статику отдавал отдельный контейнер с nginx, он же проксировал /api на
 * backend. Теперь контейнер один: Spring отдаёт и API, и файлы фронтенда на одном
 * порту, поэтому фронтенду не нужен ни прокси, ни CORS — запросы идут на тот же
 * origin.
 *
 * Каталог со статикой задаётся app.static-dir (в образе — /app/static, куда
 * Dockerfile копирует frontend/dist-prebuilt). Если каталога нет, приложение
 * продолжает работать как чистое API — это нормальный режим для локальной
 * разработки, когда фронтенд поднят отдельно через `npm run dev`.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String location = fileUrl(props.getStaticDir());

        // Файлы в /assets/ Vite именует с хешем содержимого (index-BqbatdKU.js), поэтому
        // их можно кэшировать надолго: при пересборке меняется имя файла, а не содержимое
        // по старому имени. Без этого Spring Security проставил бы no-store на всё подряд
        // и браузер качал бы бандл заново на каждой загрузке страницы.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(location + "assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        // Всё остальное: обычные файлы (index.html, иконки), а для неизвестных путей —
        // index.html, чтобы работали маршруты React Router (/history, /tasks/..., /admin/...).
        // Маршруты API сюда попадать не должны: если контроллера нет, корректный ответ —
        // 404 в формате API, а не HTML-страница приложения.
        registry.addResourceHandler("/**")
                .addResourceLocations(location)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                            throws IOException {
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                            return null;
                        }
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        Resource index = location.createRelative("index.html");
                        return index.exists() && index.isReadable() ? index : null;
                    }
                });
    }

    /** file:-URL каталога с завершающим слэшем — в таком виде его ждёт addResourceLocations. */
    private String fileUrl(String dir) {
        String absolute = Path.of(dir).toAbsolutePath().normalize().toString().replace('\\', '/');
        return "file:" + (absolute.endsWith("/") ? absolute : absolute + "/");
    }
}
