El worker de iOS en el commit `cd79b4f` incluye `is_alarm` en APNs. Su flujo FCM usa todavía `claim_android_pushes` sin esa columna y publica avisos con el canal estándar.

Para activar el modo alarma de Android en el mismo servicio de Render:

1. Publicar la versión Android con `soundisco_milestone_alarms` y el recurso `milestone_alarm.wav`.
2. Ejecutar `android_alarm_notifications.sql` en Supabase, después de las migraciones de notificaciones y `android_push.sql`. La función nueva conserva los arrendamientos, reintentos y permisos de la cola existente.
3. En la raíz del repositorio iOS que contiene `Supabase/push-worker/worker.mjs`, aplicar `git apply` al archivo `android-alarm-worker.patch`. La referencia de este parche es `cd79b4f`.
4. Volver a desplegar ese mismo servicio en Render. No se necesita otro servicio ni otra credencial Firebase.

El payload FCM pasa a ser de datos y mantiene prioridad alta. Incluye `notification_id`, `recipient_id`, `title`, `body`, `is_alarm` y `channel_id`. Así `FirebaseMessagingService` también selecciona el canal correcto cuando la app está en segundo plano. Los avisos normales conservan el canal `soundisco_notifications`.

El endpoint `/health` informa `androidMilestoneAlarm: true` y `fcmQueueRPC: claim_android_pushes_v2` después de actualizar el worker.

Antes de desplegar, ejecutar `node --test worker.test.mjs android.test.mjs android-alarm.sql.test.mjs` desde `Supabase/push-worker/`, con las dependencias instaladas. Copiar también `android-alarm.sql.test.mjs` a esa carpeta para validar la migración en PostgreSQL local mediante PGlite.

Prueba con un dispositivo real y una cuenta del equipo: bloquear la pantalla, provocar una notificación cuyo `payload.is_alarm` sea `true`, comprobar el sonido y la vibración, abrir el hito y luego enviar un aviso normal. No reenviar alarmas de otra cuenta al dispositivo.

Android 14 y posteriores requieren autorización especial para abrir avisos a pantalla completa. La app comprueba `canUseFullScreenIntent()` antes de usarlos. Si esa autorización no está disponible, muestra la alarma mediante el canal de importancia alta. El sonido, el volumen, las notificaciones y No molestar siguen sujetos a los ajustes del teléfono.

La pantalla completa abre un aviso de alarma; confirmar el hito sigue siendo una acción explícita del colaborador. Si la app usa una versión anterior al soporte del canal de alarmas, actualizarla antes de desplegar este payload.
