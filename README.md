# SounDisco Android

Aplicación Kotlin con Jetpack Compose y Material 3. Esta entrega implementa autenticación por correo, registro con metadatos de empleado, comprobación de aprobación y el scaffold de Home con consultas reales.

## Estructura

Los paquetes están dentro de `app/src/main/java/edu/ucne/soundsicoappandroid`:

| Paquete | Responsabilidad |
| --- | --- |
| `app` | Application, contenedor de dependencias, actividad y navegación según sesión. |
| `core` | Cliente Supabase compartido, tema, componentes y mensajes de error. |
| `domain` | Entidades, validación, contratos y casos de uso. No importa Android ni Supabase. |
| `data` | Repositorios Supabase, DTO, conversiones y preferencia de correo recordado. |
| `presentation` | State, Intent, ViewModel y Screen de Login, SignUp y Home. |

Las vistas envían intents. Los ViewModels ejecutan casos de uso y publican estados inmutables mediante StateFlow. Compose observa el estado respetando el ciclo de vida. La sesión decide la navegación y destruye los ViewModels del usuario anterior al cambiar de cuenta. La rotación conserva los ViewModels; las contraseñas no se guardan en SavedState ni en preferencias.

## Supabase

Se utiliza `io.github.jan-tennert.supabase`, con Auth, Postgrest y el motor OkHttp de Ktor. Las versiones están centralizadas en `gradle/libs.versions.toml`.

La configuración predeterminada corresponde a la URL y clave publicable incluidas en el código iOS. Puede sustituirse mediante las propiedades Gradle `SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY`, en el archivo personal de propiedades Gradle o con argumentos `-P`. No se utiliza una clave administrativa.

El registro envía `nombre_usuario`, `nombre_completo`, `telefono` y `cargo`. Los roles y la aprobación siguen bajo control del servidor. Se espera registro con sesión inmediata, igual que en iOS; si el servidor requiere confirmación de correo, se comunica el fallo de configuración y no se presenta el registro como completado.

Home comprueba `my_account_access` y consulta `perfiles`. Las cuentas pendientes o rechazadas ven el estado de acceso y pueden consultarlo nuevamente o cerrar sesión. Las cuentas aprobadas tienen Inicio, Mensajes, Calendario y Perfil. Las consultas usan `asignacion_equipo`, `asignaciones`, `hitos_itinerario`, `hitos_colaboradores`, `asignacion_supervisores` y `comunicados`, bajo las políticas existentes de Supabase. El calendario incluye las fechas de los hitos y el plazo de la asignación.

Home distingue el rol obtenido de `perfiles` después de verificar el acceso aprobado. `AdminDashboardView` carga métricas reales con `admin_dashboard_summary`, asignaciones paginadas con `admin_assignments_page` y solicitudes pendientes con `admin_list_accounts`. Permite aprobar o rechazar cuentas mediante `admin_review_account`, después de una confirmación explícita. Los filtros y la búsqueda del administrador se ejecutan en el servidor; la búsqueda espera 300 ms y cancela solicitudes anteriores.

`EmployeeAgendaView` consulta únicamente las asignaciones vinculadas al empleado y muestra la agenda del día según la zona horaria del dispositivo. “Ver todas” elimina el filtro de fecha. Las tarjetas muestran el siguiente hito personal, ubicación, prioridad y estado. El calendario administrativo consulta todas las páginas, independientemente del filtro y la página visibles en Inicio.

Los accesos rápidos de esta entrega son aprobaciones y calendario. El botón de creación de asignaciones que aparece en la referencia iOS pertenece al CRUD administrativo y no está incluido en esta etapa. Tampoco se incorporan todavía confirmaciones de hitos, recuperación de contraseña, biometría, edición de perfil ni notificaciones. No se modificaron Supabase ni Render.

Los enlaces legales conservan el aviso que presenta iOS: los documentos corporativos deben solicitarse a administración. El repositorio original no proporciona esos documentos ni URLs oficiales.

## Diseño

Se reutiliza el logo original y se conserva el acento rojo, la cabecera de acceso, el orden de los campos de registro y las cuatro pestañas. Material 3 aporta formularios, diálogos, calendario y navegación. Hay temas claro y oscuro y formularios desplazables con teclado. La semejanza se basa en el código de iOS; no se ha medido un porcentaje mediante comparación de capturas de ambas plataformas.

## Verificación

En Windows, con Android SDK y Java disponibles:

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew.bat :app:connectedDebugAndroidTest
```

La segunda orden requiere un emulador o dispositivo. Las pruebas usan repositorios de prueba y estados locales; no registran cuentas ni envían datos a producción. Para validar la integración completa se necesita iniciar sesión manualmente con una cuenta de prueba aprobada y otra pendiente.

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.
