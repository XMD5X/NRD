# Развёртывание в Kubernetes

**Рекомендуемый способ — Helm-чарт в `helm/nrd-business-panel/`** (см. его
README.md): та же топология, что и ниже, плюс HPA (для web), 
PodDisruptionBudget, NetworkPolicy между всеми тремя сервисами, security-
контексты (allowPrivilegeEscalation: false, drop ALL capabilities,
seccompProfile RuntimeDefault) и параметризация через `values.yaml` под
разные окружения — то, чего не было в плоских манифестах ниже.

Плоские манифесты в этой папке (`00-namespace.yaml` … `06-ingress.yaml`)
остаются как более простой вариант без Helm — например, для быстрой
локальной проверки одной командой `kubectl apply -k .` без разбора values.
Оба варианта разворачивают идентичный набор объектов на уровне Deployment/
Service/PVC; чарт — их надмножество.

Манифесты в этой папке — перенос стенда из `docker-compose.yml` в k8s. Состав
сервисов и их назначение те же (см. `README.md` в корне репозитория, раздел
про Docker Compose), просто оформлены как k8s-объекты. Соответствие:

| docker-compose.yml   | k8s                                                      |
|-----------------------|-----------------------------------------------------------|
| `postgres` (сервис)   | `03-postgres.yaml` — StatefulSet + headless Service        |
| `backend` (сервис)    | `04-backend.yaml` — Deployment (1 реплика) + Service + PVC |
| volume `pgdata`       | volumeClaimTemplate в StatefulSet postgres                 |
| volume `backend_data` | PVC `backend-data` (ReadWriteOnce)                          |
| `ports: 18080:8080`   | `06-ingress.yaml` (внешний домен) + Service `backend` внутри кластера |
| `.env` (`POSTGRES_PASSWORD`, `APP_JWT_SECRET`) | Secret `adminpanel-secrets` (см. `01-secret.example.yaml`) |

## Порядок разворачивания

1. **Соберите и запушьте образ** в ваш registry. Образ теперь один: отдельного
   образа со статикой и nginx больше нет, фронтенд попадает внутрь образа
   backend отдельным контекстом сборки:

   ```
   # Статику фронтенда собираем ДО docker build (npm ci && npm run build в
   # frontend/, результат — в frontend/dist-prebuilt): сборка vite внутри
   # контейнера на этой машине стабильно падает, см. README в корне.
   docker build --build-context frontend=./frontend \
                -t <registry>/nrd-business-panel/backend:latest ./backend
   docker push <registry>/nrd-business-panel/backend:latest
   ```

   Поправьте `image:` в `04-backend.yaml`, если используете не
   `repo.dev002.local/nrd-business-panel/...`.

2. **Создайте namespace и секреты**:

   ```
   kubectl apply -f 00-namespace.yaml

   kubectl create secret generic adminpanel-secrets \
     -n nrd-business-panel \
     --from-literal=POSTGRES_PASSWORD='<случайный-пароль>' \
     --from-literal=APP_JWT_SECRET="$(openssl rand -hex 32)"
   ```

3. **Поправьте домен** в `06-ingress.yaml` (`host:` и `tls.hosts`) и
   заведите TLS-сертификат (`adminpanel-tls`) — через cert-manager или руками:

   ```
   kubectl create secret tls adminpanel-tls \
     -n nrd-business-panel \
     --cert=server.crt --key=server.key
   ```

4. **Примените остальные манифесты**:

   ```
   kubectl apply -k .
   # либо по одному, без kustomize:
   kubectl apply -f 02-configmap-app.yaml -f 03-postgres.yaml \
                 -f 04-backend.yaml -f 06-ingress.yaml
   ```

## О чём стоит знать перед прод-эксплуатацией

- **backend намеренно `replicas: 1`.** Онлайн-статус пользователей хранится в
  памяти процесса (см. основной README, раздел про индикацию пользователей
  онлайн), редактирование скриптов (`/admin/scripts`,
  `ScriptService.updateContent`) пишет файл + `.bak` напрямую на диск тома
  `backend-data`, а счётчик попыток входа (`LoginRateLimitFilter`) живёт в
  памяти и при нескольких репликах станет лимитом «на реплику». Масштабирование
  требует доработки приложения: вынос этого состояния в общее хранилище.
- **backend-контейнер стартует от root на долю секунды** (см.
  `backend/docker-entrypoint.sh`) и сам понижает привилегии до `appuser`.
  Если в кластере включён Pod Security Admission профиля `restricted`
  (требует `runAsNonRoot: true` на уровне пода), этот механизм не сработает —
  потребуется либо исключение для namespace, либо переделка Dockerfile.
- **TLS** терминируется на Ingress реальным сертификатом из Secret
  `adminpanel-tls`; до пода трафик идёт по HTTP на порт 8080. Приложение само
  TLS не поднимает — если он нужен до пода, это настраивается через
  `server.ssl.*` в Spring Boot и аннотацию `backend-protocol: HTTPS` у Ingress.
- **HPA/autoscaling, PodDisruptionBudget, NetworkPolicy** — в этих плоских
  манифестах не добавлены, здесь только прямой перенос текущего стенда. Все
  они уже есть в Helm-чарте (`helm/nrd-business-panel/`) — используйте его,
  если это нужно, вместо ручного добавления сюда.

## CI/CD

Сборка образов и деплой через `helm upgrade` вынесены в `.gitlab-ci.yml` в
корне репозитория (job'ы `Build backend image`, `Build frontend image`,
`Deploy (helm upgrade)`, все — ручной запуск, `when: manual`, в дополнение к
уже существующему job'у зеркалирования с GitHub). См. комментарии в самом
файле — там же пояснение, почему сборка образов через kaniko, а не
Docker-in-Docker.
