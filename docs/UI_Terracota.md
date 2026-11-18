# UI web Terracota de YAESTÁ

Se aplicó la primera propuesta aprobada: terracota sobrio, marfil cálido, curvas amplias, tarjetas fotográficas y navegación superior. No se utilizó una imagen del mockup como página: la estructura está implementada en Angular, HTML y CSS.

Servicios locales muestra un taller de sastrería. Las fotografías generadas son referenciales y se identifican como tales. Están guardadas en public/images/negocios-atlas.png; la web las usa localmente, sin depender de enlaces externos. El reloj del logo y favicon son SVG nativos.

## Rutas conectadas

- Explorar e inicio: /
- Ficha y servicios de cada negocio: /negocios/:id
- Mi turno y consulta de alternativas desde un turno activo: /turnos
- Novedades de negocios seguidos: /siguiendo
- Próximas visitas sugeridas: /calendario
- Membresías: /plus
- Perfil: /perfil
- Login y registro: /login y /registro
- Propietarios: /panel-negocio/:id y /servicios-negocio/:id

Las rutas privadas mantienen los controles de sesión y de rol. Se corrigió el retorno desde login para Plus y Mis negocios. El menú de cuenta incluye perfil, membresía y cerrar sesión.

El catálogo se obtiene de la API real y permite buscar por nombre/categoría/distrito, elegir distrito y categoría, ordenar A-Z y limpiar filtros. Las categorías son restaurantes, cafeterías, barberías, belleza, mascotas, talleres, lavanderías y servicios locales. Se reconocen categorías actuales como Veterinaria y Salón de uñas. Las categorías sin locales muestran un estado vacío. No se inventaron restaurantes, precios, distancias, tiempos de espera ni horarios de apertura.

Para restaurantes, el concepto propuesto es lista de espera para mesas; la lógica específica de tamaño de grupo, mesas y capacidad requiere desarrollo adicional antes de ofrecerla como implementada. Esta actualización incorpora su categoría visual.

Mis negocios usa las publicaciones de la API Plus de la actualización previa; esa API debe estar ejecutando el backend actualizado. Los cobros y el saldo promocional conservan el estado pendiente documentado anteriormente.

## Comprobación

Compilación de producción Angular correcta y seis pruebas de navegación/filtros aprobadas. En navegador se verificaron búsqueda, categoría Belleza, apertura de la ficha real de Brillo Studio y redirección de Plus hacia login con destino conservado. Se revisó escritorio y móvil, sin desbordamiento horizontal de la página. No se modificó la base de datos.

Fotografías generadas con la herramienta de imágenes integrada; prompt: contacto fotográfico regular de 4 columnas y 2 filas, sin bordes ni texto, con restaurante peruano, cafetería, barbería, salón de manicure, grooming, taller de bicicletas, lavandería y sastrería; luz natural cálida, tonos terracota y marfil, fotografías editoriales realistas. El último recuadro debe mostrar maniquí, máquina de coser y telas, nunca una iglesia o monumento.


## Unificación final de todas las vistas

Login y registro usan una composición de imagen y formulario; en móvil el formulario aparece primero. Calendario, turnos, perfil, ficha del negocio, Plus, seguidos, cola y gestión de servicios usan VistaCabecera, una cabecera fotográfica compartida con el reloj de la marca. Las imágenes de fichas se eligen según la categoría del local.

Se unificaron tipografía, botones primarios/secundarios, campos, tarjetas, mensajes y estados en yaesta-pages.css. Los días del calendario conservan fondo neutro y solo la fecha seleccionada se resalta. La configuración de la cola y el registro presencial se despliegan sin ocultar las operaciones de atención. Un turno único se centra para mantener el equilibrio visual. Se corrigió el enlace a Negocios seguidos dentro del canal.

Se configuró proxy.conf.json en Angular para que npm start envíe /api a 127.0.0.1:8080. La API debe estar encendida para iniciar sesión y consultar datos reales. No se arrancó ni modificó la base real durante la revisión de diseño.

Verificación final: build de producción aprobado y seis pruebas Angular aprobadas. Revisión visual con datos de prueba en origen local aislado, sin solicitudes de escritura: calendario (cambio de mes y filtro de fecha), cola (desplegar configuración), servicios (abrir edición), login, registro, turnos, perfil, Plus, ficha y seguidos. Se revisaron login, calendario y panel a ancho móvil sin desbordamiento horizontal de la página. El servidor de pruebas no forma parte del código de producción.
