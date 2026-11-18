-- YAESTA - SQL Server 2019 o posterior.
-- Ejecutar todo en SQL Server Management Studio con una cuenta que pueda crear bases.
-- No borra tablas ni datos. En una base existente no sustituye una migracion de esquema.
-- Cuentas y negocios de demostracion. No usar estas claves en produccion.

USE [master];
GO

IF DB_ID(N'yaesta') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [yaesta]');
END;
GO

USE [yaesta];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

BEGIN TRY
    IF DB_NAME() <> N'yaesta'
        THROW 50000, 'Selecciona la base yaesta antes de continuar.', 1;
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.usuario (
            id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuario PRIMARY KEY,
            nombre NVARCHAR(100) NOT NULL,
            correo VARCHAR(120) NOT NULL CONSTRAINT uq_usuario_correo UNIQUE,
            clave_hash VARCHAR(100) NOT NULL,
            rol VARCHAR(15) NOT NULL,
            activo BIT NOT NULL CONSTRAINT df_usuario_activo DEFAULT (1),
            acepta_recordatorios BIT NOT NULL CONSTRAINT df_usuario_recordatorios DEFAULT (0),
            CONSTRAINT ck_usuario_rol CHECK (rol IN ('CLIENTE','NEGOCIO'))
        );
    END;

    IF OBJECT_ID(N'dbo.negocio', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.negocio (
            id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_negocio PRIMARY KEY,
            nombre NVARCHAR(100) NOT NULL,
            categoria NVARCHAR(60) NOT NULL,
            distrito NVARCHAR(60) NOT NULL,
            propietario_id BIGINT NULL,
            acepta_turnos BIT NOT NULL CONSTRAINT df_negocio_turnos DEFAULT (0),
            tolerancia_minutos INT NOT NULL CONSTRAINT df_negocio_tolerancia DEFAULT (5),
            CONSTRAINT fk_negocio_propietario FOREIGN KEY (propietario_id) REFERENCES dbo.usuario(id),
            CONSTRAINT ck_negocio_tolerancia CHECK (tolerancia_minutos BETWEEN 1 AND 30)
        );
    END;

    IF OBJECT_ID(N'dbo.servicio', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.servicio (
            id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_servicio PRIMARY KEY,
            negocio_id BIGINT NOT NULL,
            nombre NVARCHAR(100) NOT NULL,
            precio DECIMAL(10,2) NOT NULL,
            duracion_minutos INT NOT NULL,
            dias_recordatorio INT NULL,
            activo BIT NOT NULL CONSTRAINT df_servicio_activo DEFAULT (1),
            CONSTRAINT fk_servicio_negocio FOREIGN KEY (negocio_id) REFERENCES dbo.negocio(id),
            CONSTRAINT uq_servicio_negocio UNIQUE (id, negocio_id),
            CONSTRAINT ck_servicio_precio CHECK (precio >= 0),
            CONSTRAINT ck_servicio_duracion CHECK (duracion_minutos > 0),
            CONSTRAINT ck_servicio_dias CHECK (dias_recordatorio IS NULL OR dias_recordatorio > 0)
        );
    END;

    IF OBJECT_ID(N'dbo.turno', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.turno (
            id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_turno PRIMARY KEY,
            negocio_id BIGINT NOT NULL,
            servicio_id BIGINT NOT NULL,
            cliente_id BIGINT NULL,
            nombre_presencial NVARCHAR(100) NULL,
            estado VARCHAR(15) NOT NULL,
            origen VARCHAR(20) NOT NULL,
            solicitado_en DATETIME2(6) NOT NULL,
            llegada_en DATETIME2(6) NULL,
            llamado_en DATETIME2(6) NULL,
            inicio_en DATETIME2(6) NULL,
            fin_en DATETIME2(6) NULL,
            tolerancia_minutos INT NOT NULL,
            precio_servicio DECIMAL(10,2) NOT NULL,
            CONSTRAINT fk_turno_negocio FOREIGN KEY (negocio_id) REFERENCES dbo.negocio(id),
            CONSTRAINT fk_turno_servicio FOREIGN KEY (servicio_id, negocio_id)
                REFERENCES dbo.servicio(id, negocio_id),
            CONSTRAINT fk_turno_cliente FOREIGN KEY (cliente_id) REFERENCES dbo.usuario(id),
            CONSTRAINT ck_turno_estado CHECK (
                estado IN ('EN_ESPERA','LLAMADO','ATENDIENDO','ATENDIDO','CANCELADO','AUSENTE')
            ),
            CONSTRAINT ck_turno_origen CHECK (
                origen IN ('BUSQUEDA','RECORDATORIO','PROMOCION','PRESENCIAL')
            ),
            CONSTRAINT ck_turno_identidad CHECK (
                (origen = 'PRESENCIAL' AND cliente_id IS NULL
                    AND nombre_presencial IS NOT NULL AND LEN(LTRIM(RTRIM(nombre_presencial))) > 0)
                OR
                (origen <> 'PRESENCIAL' AND cliente_id IS NOT NULL AND nombre_presencial IS NULL)
            ),
            CONSTRAINT ck_turno_tolerancia CHECK (tolerancia_minutos BETWEEN 1 AND 30),
            CONSTRAINT ck_turno_precio CHECK (precio_servicio >= 0)
        );
    END;

    IF OBJECT_ID(N'dbo.recordatorio', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.recordatorio (
            id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_recordatorio PRIMARY KEY,
            turno_id BIGINT NOT NULL CONSTRAINT uq_recordatorio_turno UNIQUE,
            fecha_programada DATE NOT NULL,
            estado VARCHAR(15) NOT NULL CONSTRAINT df_recordatorio_estado DEFAULT ('PENDIENTE'),
            enviado_en DATETIME2(6) NULL,
            CONSTRAINT fk_recordatorio_turno FOREIGN KEY (turno_id) REFERENCES dbo.turno(id),
            CONSTRAINT ck_recordatorio_estado CHECK (
                estado IN ('PENDIENTE','PROCESANDO','ENVIADO','CANCELADO','ERROR')
            )
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.negocio') AND name=N'ix_negocio_propietario')
        CREATE INDEX ix_negocio_propietario ON dbo.negocio(propietario_id);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.turno') AND name=N'ix_turno_cola')
        CREATE INDEX ix_turno_cola ON dbo.turno(negocio_id, estado, id);

    -- Una cuenta no puede tener dos turnos activos en el mismo negocio.
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.turno') AND name=N'uq_turno_activo_cliente')
        CREATE UNIQUE INDEX uq_turno_activo_cliente ON dbo.turno(negocio_id, cliente_id)
        WHERE cliente_id IS NOT NULL AND estado IN ('EN_ESPERA','LLAMADO','ATENDIENDO');

    -- Esta version permite un solo turno llamado o en atencion por negocio.
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.turno') AND name=N'uq_turno_en_atencion')
        CREATE UNIQUE INDEX uq_turno_en_atencion ON dbo.turno(negocio_id)
        WHERE estado IN ('LLAMADO','ATENDIENDO');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.recordatorio') AND name=N'ix_recordatorio_pendiente')
        CREATE INDEX ix_recordatorio_pendiente ON dbo.recordatorio(estado, fecha_programada);

    -- Los hashes BCrypt se generaron para las claves de prueba del README.
    -- Si la cuenta ya existe, no se modifica su nombre, rol, clave ni preferencias.
    IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE correo='fatima@yaesta.test')
        INSERT dbo.usuario(nombre, correo, clave_hash, rol, activo, acepta_recordatorios)
        VALUES (N'Fatima de prueba', 'fatima@yaesta.test',
            '$2a$10$cv8/FZE.WyIZ.ekjSfsQ1el3QrGGOtaMKQz18uztuCWmwW/IJb3Yi', 'CLIENTE', 1, 1);

    IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE correo='brillo@yaesta.test')
        INSERT dbo.usuario(nombre, correo, clave_hash, rol, activo, acepta_recordatorios)
        VALUES (N'Dueña de Brillo Studio', 'brillo@yaesta.test',
            '$2a$10$mw6OcvGkq8knpHCNFOVpHeRySnljZQo63oklRll8tE8yeLk7dWE5a', 'NEGOCIO', 1, 0);

    DECLARE @Propietario BIGINT = (SELECT id FROM dbo.usuario WHERE correo='brillo@yaesta.test');

    IF NOT EXISTS (SELECT 1 FROM dbo.negocio WHERE nombre=N'Huella Sana' AND distrito=N'Trujillo')
        INSERT dbo.negocio(nombre,categoria,distrito,propietario_id,acepta_turnos,tolerancia_minutos)
        VALUES (N'Huella Sana',N'Veterinaria',N'Trujillo',NULL,0,5);

    IF NOT EXISTS (SELECT 1 FROM dbo.negocio WHERE nombre=N'Estudio 28' AND distrito=N'Víctor Larco Herrera')
        INSERT dbo.negocio(nombre,categoria,distrito,propietario_id,acepta_turnos,tolerancia_minutos)
        VALUES (N'Estudio 28',N'Barbería',N'Víctor Larco Herrera',NULL,0,5);

    IF NOT EXISTS (SELECT 1 FROM dbo.negocio WHERE nombre=N'Brillo Studio' AND distrito=N'Trujillo')
        INSERT dbo.negocio(nombre,categoria,distrito,propietario_id,acepta_turnos,tolerancia_minutos)
        VALUES (N'Brillo Studio',N'Salón de uñas',N'Trujillo',@Propietario,1,5);

    DECLARE @Brillo BIGINT = (SELECT TOP (1) id FROM dbo.negocio WHERE nombre=N'Brillo Studio' AND distrito=N'Trujillo' ORDER BY id);
    DECLARE @Estudio BIGINT = (SELECT TOP (1) id FROM dbo.negocio WHERE nombre=N'Estudio 28' AND distrito=N'Víctor Larco Herrera' ORDER BY id);
    DECLARE @Huella BIGINT = (SELECT TOP (1) id FROM dbo.negocio WHERE nombre=N'Huella Sana' AND distrito=N'Trujillo' ORDER BY id);

    IF NOT EXISTS (SELECT 1 FROM dbo.servicio WHERE negocio_id=@Brillo AND nombre=N'Retoque de uñas')
        INSERT dbo.servicio(negocio_id,nombre,precio,duracion_minutos,dias_recordatorio,activo)
        VALUES (@Brillo,N'Retoque de uñas',35.00,45,15,1);

    IF NOT EXISTS (SELECT 1 FROM dbo.servicio WHERE negocio_id=@Estudio AND nombre=N'Corte de cabello')
        INSERT dbo.servicio(negocio_id,nombre,precio,duracion_minutos,dias_recordatorio,activo)
        VALUES (@Estudio,N'Corte de cabello',25.00,30,21,1);

    IF NOT EXISTS (SELECT 1 FROM dbo.servicio WHERE negocio_id=@Huella AND nombre=N'Consulta general')
        INSERT dbo.servicio(negocio_id,nombre,precio,duracion_minutos,dias_recordatorio,activo)
        VALUES (@Huella,N'Consulta general',40.00,30,NULL,1);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- Resumen sin contraseñas ni hashes.
SELECT id,nombre,correo,rol,acepta_recordatorios FROM dbo.usuario
WHERE correo IN ('fatima@yaesta.test','brillo@yaesta.test');

SELECT n.id,n.nombre,n.distrito,n.propietario_id,n.acepta_turnos,
       s.id AS servicio_id,s.nombre AS servicio,s.precio,s.duracion_minutos,s.dias_recordatorio
FROM dbo.negocio n LEFT JOIN dbo.servicio s ON s.negocio_id=n.id
ORDER BY n.id,s.id;
GO
