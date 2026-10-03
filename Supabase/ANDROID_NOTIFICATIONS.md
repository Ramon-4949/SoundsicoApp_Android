# Activar notificaciones Android de SounDisco

## Estado de esta entrega

El proyecto Android original contiene la integración FCM, registro de instalaciones, renovación del token, permiso de Android 13+, canal de avisos, campanita con contador, historial Realtime y navegación desde una notificación.

El worker Android y la migración están preparados y probados localmente. No se han aplicado a Supabase ni desplegado a Render. No se ha enviado una notificación real porque falta la configuración del proyecto Firebase.

La app compila sin google-services.json para poder seguir trabajando con el historial. Para activar push, hay que añadir el archivo real y volver a compilar.

## 1. Firebase y Android Studio

1. Abre Firebase Console y crea o selecciona el proyecto para SounDisco.
2. Añade una aplicación Android con este nombre de paquete exacto: edu.ucne.soundsicoappandroid.
3. Descarga google-services.json y guárdalo en C:/Proyectos programacion/Soundisco app Kotlin/app/google-services.json.
4. Sincroniza Gradle y ejecuta la aplicación. El plugin de Google Services se aplica automáticamente cuando existe ese archivo.
5. Inicia sesión y acepta el permiso de notificaciones. Usa un teléfono con Google Play Services o un emulador con Google APIs/Google Play.
6. Si denegaste el permiso, entra a la campanita y pulsa Configurar avisos del dispositivo.

Para FCM no hace falta configurar SHA-1 como requisito de esta integración. La cuenta de servicio del servidor es un archivo diferente a google-services.json.

Documentación: https://firebase.google.com/docs/cloud-messaging/android/get-started

## 2. Supabase

Ejecuta el contenido completo de Supabase/android_push.sql en el SQL Editor del proyecto usado por iOS.

Prerrequisitos: las migraciones actuales de notificaciones y control de accesos del repositorio iOS deben estar aplicadas. La migración extiende check_account_access con dos rutas de registro Android, conservando el bloqueo de datos operativos de cuentas pendientes. Si has personalizado ese guard en tu servidor, integra esas dos rutas en tu versión antes de desplegar.

Crea:

- android_push_private.devices: instalaciones y tokens FCM.
- android_push_private.outbox: cola por notificación y dispositivo.
- register_android_push_device y unregister_android_push_device: registro autenticado.
- claim_android_notification_pushes y finish_android_notification_push: consumo exclusivo del servidor.
- Un trigger sobre notificaciones_app que añade cada aviso nuevo a la cola Android.

No vuelve a generar eventos ni reenvía el historial. La cola APNs existente sigue funcionando. La migración se puede ejecutar de nuevo.

No vuelvas a ejecutar las migraciones antiguas de notificaciones de iOS: podrían reemplazar las reglas posteriores.

## 3. Credencial del servidor Firebase

1. Firebase Console → Configuración del proyecto → Cuentas de servicio.
2. Genera una clave privada para Firebase Admin SDK.
3. En el nuevo servicio de Render, añádela como Secret File con nombre firebase-service-account.json.
4. Configura GOOGLE_APPLICATION_CREDENTIALS=/etc/secrets/firebase-service-account.json.
5. Comprueba que Firebase Cloud Messaging API está habilitada para ese proyecto y que la cuenta de servicio tiene permiso para enviar mensajes.

Esta clave privada solo va en Render. No la copies a Android, al repositorio ni a los recursos de la app.

Documentación: https://firebase.google.com/docs/cloud-messaging/send/admin-sdk
Secret files de Render: https://render.com/docs/configure-environment-variables

## 4. Render

Esta entrega añade un servicio Android separado que usa el mismo Supabase. Conserva el servicio APNs actual.

Sube los archivos del proyecto a tu repositorio y crea un Web Service de Node en Render:

| Campo | Valor |
| --- | --- |
| Root Directory | Supabase/android-push-worker |
| Node | 22 o posterior |
| Build Command | corepack enable && pnpm install --frozen-lockfile --prod |
| Start Command | node worker.mjs |
| Health Check Path | /health |

Variables de entorno:

| Variable | Valor |
| --- | --- |
| SUPABASE_URL | La misma URL de Supabase que usa la app |
| SUPABASE_SERVICE_ROLE_KEY | La clave de servidor de Supabase, nunca la publishable/anon |
| GOOGLE_APPLICATION_CREDENTIALS | /etc/secrets/firebase-service-account.json |

Render proporciona PORT. No necesitas configurar una URL de Render dentro de Android: el teléfono registra su token en Supabase y Render envía a FCM.

El proceso consulta la cola cada 10 segundos, procesa lotes de 50, reintenta errores y descarta tokens que FCM declara no registrados. Un error de credenciales no borra dispositivos. Cada entrega usa una concesión temporal y una clave única por aviso e instalación.

Usa una instancia que permanezca activa. Los Web Services gratuitos de Render se suspenden por inactividad; esto pausa el envío y no sirve para avisos operativos continuos. El worker APNs existente debe seguir ejecutando generate_notification_reminders, como hace el código iOS original. El worker Android consume sus resultados y no duplica esa generación.

Documentación: https://render.com/docs/free

## Reglas compartidas con iOS

Referencia revisada: Ramon-4949/SoundiscoApp, commit 0a21f15.

Los triggers de Supabase siguen siendo la única fuente de títulos, mensajes y destinatarios. Android respeta las reglas realmente desplegadas allí:

- Asignaciones nuevas, modificadas, canceladas, retiradas y completadas.
- Avisos de prioridad alta y cambios de estado.
- Confirmaciones de hitos para administradores según milestone_notifications_admin_only.sql.
- Comunicados nuevos, modificados o eliminados, excluyendo al autor.
- Registro de usuarios para administradores aprobados.
- Aprobación o rechazo de cuenta para el solicitante, incluso si aún no puede entrar a la app.
- Recordatorios previos al vencimiento y avisos de vencimiento generados por el worker existente.
- La ventana de recordatorio de 15 minutos no se convierte en una tolerancia para confirmar hitos.

El historial incluye los filtros de iOS Todas, Sin leer, Asignaciones y Comunicados, y los filtros operativos Vencidas, Pendientes y Completadas de la referencia visual. Marca un aviso como leído al abrirlo o permite marcar todos. Conserva avisos de contenido eliminado como información.

## Prueba completa

1. Aplica el SQL, despliega Render y añade google-services.json.
2. Ejecuta Android e inicia sesión como empleado.
3. Desde otra cuenta crea una asignación para ese empleado. Usa un evento nuevo: el historial no se reenvía.
4. Comprueba el aviso con la app visible, en segundo plano y cerrada normalmente.
5. Toca el aviso y comprueba el detalle. Si el destino fue eliminado, debe aparecer el texto informativo.
6. Comprueba el contador y Marcar todas como leídas.
7. Cierra sesión y entra con otra cuenta: no deben mostrarse ni abrirse avisos de la cuenta anterior.
8. Registra un usuario pendiente, apruébalo desde otra cuenta y comprueba el aviso de aprobación.
9. Confirma un hito desde Android y verifica que el administrador recibe el aviso según las reglas compartidas.

El envío usa mensajes FCM de datos con prioridad alta para validar el destinatario antes de mostrar el aviso. Android puede retrasar la entrega por restricciones del dispositivo; tras Forzar detención es necesario volver a abrir la app. No uses una campaña de Firebase Console para validar este flujo: prueba un evento real que genere notificaciones_app.

## Diagnóstico

- /health devuelve 200 después de completar un ciclo correctamente. Devuelve 503 si no consigue completar ciclos durante cinco minutos.
- Si el historial carga pero no hay push, revisa el registro de instalación, permiso, credencial Firebase y worker.
- Si tampoco hay historial, revisa que los triggers creen notificaciones_app para ese destinatario y las políticas de acceso.
- El endpoint de salud no genera recordatorios.
- No publiques tokens ni claves al compartir logs.
- Hay hasta ocho intentos por trabajo, con espera creciente. Solo se envían trabajos creados en las últimas 24 horas. La cola se limpia después de siete días.
- El registro de token se reintenta con WorkManager y se vuelve a programar al iniciar sesión o configurar avisos.

## Verificación local

Android: gradlew.bat testDebugUnitTest assembleDebug.

Worker: pnpm install; pnpm test.

Las pruebas SQL usan PostgreSQL local mediante PGlite. Cubren aislamiento, registro, rotación del token, bloqueo de accesos pendientes, concesiones de entrega, cierre de sesión y ausencia de reenvío histórico. No conectan con la base remota.
