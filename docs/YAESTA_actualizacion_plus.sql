-- YAESTÁ: ampliación de cola y Plus. Ejecutar en la base existente de YAESTÁ.
-- No crea usuarios ni membresías, no modifica precios ni elimina datos.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF OBJECT_ID('dbo.turno','U') IS NULL OR OBJECT_ID('dbo.negocio','U') IS NULL
    THROW 50000, 'Selecciona la base existente de YAESTA antes de ejecutar.', 1;
BEGIN TRY
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.negocio','latitud') IS NULL ALTER TABLE dbo.negocio ADD latitud float NULL;
IF COL_LENGTH('dbo.negocio','longitud') IS NULL ALTER TABLE dbo.negocio ADD longitud float NULL;
IF COL_LENGTH('dbo.negocio','disponibilidad_actualizada') IS NULL ALTER TABLE dbo.negocio ADD disponibilidad_actualizada datetime2(6) NULL;
IF COL_LENGTH('dbo.negocio','descuento_derivacion') IS NULL ALTER TABLE dbo.negocio ADD descuento_derivacion int NULL;
IF COL_LENGTH('dbo.negocio','cupos_derivacion') IS NULL ALTER TABLE dbo.negocio ADD cupos_derivacion int NULL;
IF COL_LENGTH('dbo.turno','no_llamar_antes_de') IS NULL ALTER TABLE dbo.turno ADD no_llamar_antes_de datetime2(6) NULL;
IF COL_LENGTH('dbo.turno','fin_estimado') IS NULL ALTER TABLE dbo.turno ADD fin_estimado datetime2(6) NULL;
IF COL_LENGTH('dbo.turno','orden_cola') IS NULL ALTER TABLE dbo.turno ADD orden_cola bigint NULL;
IF COL_LENGTH('dbo.turno','intercambios') IS NULL ALTER TABLE dbo.turno ADD intercambios int NULL;
IF COL_LENGTH('dbo.turno','duracion_reservada') IS NULL ALTER TABLE dbo.turno ADD duracion_reservada int NULL;
IF COL_LENGTH('dbo.turno','derivado_desde') IS NULL ALTER TABLE dbo.turno ADD derivado_desde bigint NULL;
IF COL_LENGTH('dbo.turno','descuento_aplicado') IS NULL ALTER TABLE dbo.turno ADD descuento_aplicado int NULL;
IF COL_LENGTH('dbo.turno','espera_inicial_minutos') IS NULL ALTER TABLE dbo.turno ADD espera_inicial_minutos bigint NULL;
IF OBJECT_ID('dbo.membresia','U') IS NULL
BEGIN
CREATE TABLE dbo.membresia(
id bigint IDENTITY(1,1) PRIMARY KEY, clave varchar(80) NOT NULL UNIQUE,
usuario_id bigint NOT NULL REFERENCES dbo.usuario(id), negocio_id bigint NULL REFERENCES dbo.negocio(id),
inicio datetime2(6) NOT NULL, fin datetime2(6) NOT NULL, demostracion bit NOT NULL,
CONSTRAINT CK_membresia_fechas CHECK (fin >= inicio)
);
END;
IF OBJECT_ID('dbo.seguimiento','U') IS NULL
BEGIN
CREATE TABLE dbo.seguimiento(
id bigint IDENTITY(1,1) PRIMARY KEY, cliente_id bigint NOT NULL REFERENCES dbo.usuario(id),
negocio_id bigint NOT NULL REFERENCES dbo.negocio(id), alertas bit NOT NULL,
CONSTRAINT UQ_seguimiento UNIQUE(cliente_id,negocio_id)
);
END;
IF OBJECT_ID('dbo.publicacion','U') IS NULL
BEGIN
CREATE TABLE dbo.publicacion(
id bigint IDENTITY(1,1) PRIMARY KEY, negocio_id bigint NOT NULL REFERENCES dbo.negocio(id),
titulo nvarchar(100) NOT NULL, mensaje nvarchar(1000) NOT NULL,
creada_en datetime2(6) NOT NULL, vence_en datetime2(6) NOT NULL
);
END;
IF OBJECT_ID('dbo.intercambio','U') IS NULL
BEGIN
CREATE TABLE dbo.intercambio(
id bigint IDENTITY(1,1) PRIMARY KEY, negocio_id bigint NOT NULL REFERENCES dbo.negocio(id),
origen_id bigint NOT NULL REFERENCES dbo.turno(id), destino_id bigint NOT NULL REFERENCES dbo.turno(id),
solicitante_id bigint NOT NULL REFERENCES dbo.usuario(id), receptor_id bigint NOT NULL REFERENCES dbo.usuario(id),
orden_origen bigint NOT NULL, orden_destino bigint NOT NULL, vence_en datetime2(6) NOT NULL,
estado varchar(15) NOT NULL, CONSTRAINT CK_intercambio_distinto CHECK (origen_id <> destino_id)
);
END;
IF OBJECT_ID('dbo.evento_turno','U') IS NULL
BEGIN
CREATE TABLE dbo.evento_turno(
id bigint IDENTITY(1,1) PRIMARY KEY, turno_id bigint NOT NULL REFERENCES dbo.turno(id),
actor_id bigint NULL REFERENCES dbo.usuario(id), accion varchar(40) NOT NULL,
detalle nvarchar(240) NOT NULL, fecha datetime2(6) NOT NULL
);
END;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.evento_turno') AND name='IX_evento_turno_historial') CREATE INDEX IX_evento_turno_historial ON dbo.evento_turno(turno_id,id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.publicacion') AND name='IX_publicacion_negocio') CREATE INDEX IX_publicacion_negocio ON dbo.publicacion(negocio_id,vence_en);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.intercambio') AND name='IX_intercambio_receptor') CREATE INDEX IX_intercambio_receptor ON dbo.intercambio(receptor_id,id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.seguimiento') AND name='IX_seguimiento_negocio') CREATE INDEX IX_seguimiento_negocio ON dbo.seguimiento(negocio_id);
COMMIT TRANSACTION;
END TRY
BEGIN CATCH
IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
THROW;
END CATCH;
