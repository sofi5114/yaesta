# YAESTÁ: actualización de cola y membresías

Estado: código local en C:/Users/Fatim/Downloads/yaesta. No publicado en GitHub. No se habilitaron cobros reales.

## Implementado

- Cola remota y presencial compartida, con una atención simultánea por negocio.
- Vencimiento automático del llamado al agotarse su tolerancia, solo si el negocio no confirmó la llegada. Revisión cada 15 segundos; nunca vence un turno en atención ni uno con presencia confirmada por una demora del local.
- Duración y precio del servicio conservados al emitir nuevos turnos.
- Actualización manual del tiempo restante por el negocio. Cuando una atención supera la estimación, se muestra espera por actualizar en vez de inventar cero.
- Alerta en Mis turnos cuando el inicio previsto se desplaza al menos 10 minutos respecto de la estimación inicial. Se consulta mientras la pantalla está abierta; no equivale a notificaciones push.
- Negocio Plus: canal de publicaciones, seguidores gratuitos, dejar de seguir, preferencia de avisos y máximo de tres publicaciones en 24 horas.
- Cliente Plus: propuestas de intercambio de tres minutos, aceptación de la otra persona, un cambio por turno, igual servicio y duración, solo entre miembros vigentes y antes de registrar llegada. Se conserva la identidad, el precio y el historial de cada turno. Un intercambio voluntario invalida la comparación con la espera inicial para no generar un descuento por una demora que el propio cliente provocó.
- Alternativas a 10 km de las coordenadas elegidas, con coincidencia normalizada del nombre del servicio y disponibilidad confirmada en los últimos 10 minutos. No es un catálogo semántico: nombres diferentes requieren homologación futura.
- Promoción de derivación para Negocio Plus: el negocio configura entre 0 y 30 % y hasta 100 cupos por configuración. Solo se ofrece si el turno de origen tiene una demora significativa comprobable. El local receptor financia el descuento.
- Traslado protegido: al cambiar de local, el cliente confirma entre 1 y 60 minutos para llegar. No se llama su turno antes de ese plazo; otros clientes listos pueden avanzar. Si llega antes y el negocio lo confirma, se libera la protección. La atención exacta no está garantizada.
- Cambio de local transaccional: valida otra vez disponibilidad y precio, crea el nuevo turno y cancela el anterior en una sola transacción. Si falla, ambos cambios se revierten. Cada confirmación con descuento consume un cupo; la cancelación posterior no lo repone automáticamente.
- Historial visible al cliente de llamados, llegada, atención, vencimiento, demoras, intercambios, derivaciones y cancelaciones del negocio con motivo.
- Plus tiene vigencia validada en el servidor; no se habilita por modificar el navegador.

## Precios acordados

Cliente Plus: S/9.90 mensuales, configurable con `yaesta.plus.precio-cliente=9.90`.
Negocio Plus: sin precio, `yaesta.plus.precio-negocio=`. Un precio vacío no representa un plan gratuito.

## Cobros reales: pendientes

Falta elegir Mercado Pago o Culqi y configurar la cuenta del titular. No se implementó ni activó un checkout de cobro. Antes de habilitar pagos se requiere:

1. Integración del proveedor elegido, credenciales en variables de entorno y URL HTTPS pública de notificaciones.
2. Activación de Plus a partir de un pago confirmado y consultado al proveedor; nunca desde el retorno del navegador ni desde una solicitud del cliente.
3. Eventos procesados una sola vez, validación de firma, importe, moneda y titular.
4. Renovaciones, pagos fallidos, cancelación de renovación, devolución y contracargo; conservar el período ya pagado según las condiciones acordadas.
5. Prueba completa en entorno de pruebas del proveedor antes de cobrar.

Documentación oficial consultada:
- https://www.mercadopago.com.pe/developers/es/docs/subscriptions/overview
- https://docs.culqi.com/es/documentacion/pagos-online/recurrencia/suscripciones/resumen

La única activación directa incluida es una prueba de 30 días sin cobro, deshabilitada por defecto. Para una demostración aislada: `yaesta.plus.demo=true`. Solo se utiliza una vez por cliente o negocio y no renueva. No habilitar esa propiedad en el despliegue de cobros reales.

## Decisiones y funciones todavía pendientes

- Saldo promocional: deshabilitado. Falta acordar financiamiento, cantidad, condiciones, vencimientos, canjes, reembolsos y conciliación; no se promete S/15 por el precio de S/9.90.
- Programa de recomendaciones con recompensas: pendiente de definir las recompensas y su financiamiento; todavía no implementado.
- Avisos push, SMS o campañas por correo: todavía no conectados. Los seguidores consultan las publicaciones dentro de la aplicación. La preferencia de avisos no implica que se estén enviando.
- Citas a hora fija, agendas por trabajador y atención simultánea: fuera del flujo de esta versión. El calendario actual registra próximas visitas sugeridas; no reserva horas.
- Traslado: la distancia es en línea recta, no una ruta ni una garantía de llegada. El ranking usa una aproximación de 4 min/km más espera y duración. El cliente ajusta y confirma su tiempo de viaje; se protege hasta el fin de ese plazo. Falta integrar rutas reales.
- Dirección verificable y horarios de apertura: todavía deben completarse antes de un piloto abierto. La disponibilidad actual se controla mediante recepción activada y actualización reciente del negocio. Se limita la cola a 100 turnos activos por negocio.
- Conectividad: la pantalla muestra errores de consulta y permite actualizar. No hay sincronización de operaciones sin conexión.
- No se garantiza ausencia absoluta de errores. Los escenarios cubiertos están descritos en las pruebas; faltan validaciones con negocios reales y carga de producción.

## Actualizar y ejecutar

1. Respaldar la base existente antes de una actualización real.
2. Ejecutar `YAESTA_actualizacion_plus.sql` en la base correcta. El script es aditivo e idempotente y no inserta membresías. El script se ejecutó dos veces correctamente en una base SQL Server temporal y luego se eliminó esa base. La base del usuario no se modificó.
3. Conservar la configuración local de conexión y JWT. No compartir application.properties con claves.
4. Reiniciar la API con Java 21 y Maven Wrapper. Compilar Angular con `npm run build` y ejecutar su servidor según la configuración habitual.
5. Entrar en YAESTÁ Plus desde el menú. Los dueños eligen su negocio, configuran coordenadas y confirman disponibilidad; para recibir derivaciones deben mantener la recepción de turnos activa.
6. En producción los cobros deben permanecer deshabilitados hasta completar la integración del proveedor.

## Verificación

- Pruebas Java con H2 aislado, sin usar la base personal ni enviar correos reales.
- Pruebas HTTP de autenticación, roles, validación y bloqueo de activaciones gratuitas en modo real.
- Prueba concurrente de doble aceptación de un intercambio.
- Pruebas Angular de catálogo gratuito y navegación de cliente y negocio.
- Compilación de producción Angular y revisión de diferencias.

No se ha reiniciado la aplicación real del usuario ni actualizado su base personal durante estas pruebas.

Resultados: 20 pruebas de servidor y 3 de Angular aprobadas; compilación de producción correcta. Falta verificación visual en navegador y prueba completa del proveedor de pagos.
