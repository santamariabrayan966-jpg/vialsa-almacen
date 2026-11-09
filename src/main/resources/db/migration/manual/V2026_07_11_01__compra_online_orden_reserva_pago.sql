/* =========================================================
   V2026_07_11_01__compra_online_orden_reserva_pago.sql

   Migracion manual para preparar compra online:
   - orden temporal validada;
   - reserva logica de stock y cupo;
   - intentos de pago;
   - trazabilidad hacia venta, pedido, presentacion y oferta.

   Ejecutar manualmente solo despues de respaldo y validaciones.
   ========================================================= */

/* =========================================================
   1. CREAR TABLA orden_compra_online
   ========================================================= */

CREATE TABLE orden_compra_online (
    idOrdenOnline INT NOT NULL AUTO_INCREMENT,
    codigoOrden VARCHAR(40) NOT NULL,
    idCliente INT NOT NULL,
    idSede INT NOT NULL,
    idHorarioRecojo INT NOT NULL,
    fechaRecojo DATE NOT NULL,
    horaInicioRecojo TIME NOT NULL,
    horaFinRecojo TIME NOT NULL,
    estadoOrden VARCHAR(30) NOT NULL DEFAULT 'CREADA',
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    descuentoTotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    moneda VARCHAR(10) NOT NULL DEFAULT 'PEN',
    observacion VARCHAR(255) NULL,
    fechaCreacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fechaActualizacion DATETIME NULL,
    fechaExpiracion DATETIME NULL,
    tokenIdempotencia VARCHAR(120) NOT NULL,
    PRIMARY KEY (idOrdenOnline),
    UNIQUE KEY uk_orden_codigo (codigoOrden),
    UNIQUE KEY uk_orden_idempotencia (tokenIdempotencia),
    KEY idx_orden_cliente (idCliente),
    KEY idx_orden_sede_estado (idSede, estadoOrden),
    KEY idx_orden_recojo (idSede, fechaRecojo, horaInicioRecojo, horaFinRecojo),
    CONSTRAINT fk_orden_cliente
        FOREIGN KEY (idCliente) REFERENCES clientes(idClientes),
    CONSTRAINT fk_orden_sede
        FOREIGN KEY (idSede) REFERENCES sedes(idSede),
    CONSTRAINT fk_orden_horario
        FOREIGN KEY (idHorarioRecojo) REFERENCES sede_horario_recojo(idHorarioRecojo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* =========================================================
   2. CREAR TABLA orden_compra_online_detalle
   ========================================================= */

CREATE TABLE orden_compra_online_detalle (
    idOrdenDetalle INT NOT NULL AUTO_INCREMENT,
    idOrdenOnline INT NOT NULL,
    idProducto INT NOT NULL,
    idPrecioPresentacion INT NOT NULL,
    idUnidad INT NULL,
    idOferta INT NULL,
    nombreProducto VARCHAR(120) NOT NULL,
    nombrePresentacion VARCHAR(100) NOT NULL,
    tipoCalculoVenta VARCHAR(30) NOT NULL,
    factorConversionStock DECIMAL(18,6) NOT NULL DEFAULT 1.000000,
    cantidad DECIMAL(18,6) NOT NULL,
    cantidadDescontadaStock DECIMAL(18,6) NOT NULL,
    precioNormal DECIMAL(10,2) NOT NULL,
    precioOferta DECIMAL(10,2) NULL,
    precioFinalOnline DECIMAL(10,2) NOT NULL,
    descuentoOferta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(10,2) NOT NULL,
    textoOferta VARCHAR(120) NULL,
    PRIMARY KEY (idOrdenDetalle),
    KEY idx_orden_detalle_orden (idOrdenOnline),
    KEY idx_orden_detalle_producto (idProducto),
    KEY idx_orden_detalle_presentacion (idPrecioPresentacion),
    KEY idx_orden_detalle_oferta (idOferta),
    CONSTRAINT fk_orden_detalle_orden
        FOREIGN KEY (idOrdenOnline) REFERENCES orden_compra_online(idOrdenOnline),
    CONSTRAINT fk_orden_detalle_producto
        FOREIGN KEY (idProducto) REFERENCES productos(idProducto),
    CONSTRAINT fk_orden_detalle_presentacion
        FOREIGN KEY (idPrecioPresentacion) REFERENCES producto_precio_presentacion(idPrecioPresentacion),
    CONSTRAINT fk_orden_detalle_oferta
        FOREIGN KEY (idOferta) REFERENCES producto_presentacion_oferta(idOferta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* =========================================================
   3. CREAR TABLA inventario_reserva
   ========================================================= */

CREATE TABLE inventario_reserva (
    idReserva INT NOT NULL AUTO_INCREMENT,
    codigoReserva VARCHAR(40) NOT NULL,
    idOrdenOnline INT NOT NULL,
    idCliente INT NOT NULL,
    idSede INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fechaCreacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fechaExpiracion DATETIME NOT NULL,
    fechaConfirmacion DATETIME NULL,
    fechaLiberacion DATETIME NULL,
    motivoLiberacion VARCHAR(255) NULL,
    tokenIdempotencia VARCHAR(120) NOT NULL,
    PRIMARY KEY (idReserva),
    UNIQUE KEY uk_reserva_codigo (codigoReserva),
    UNIQUE KEY uk_reserva_idempotencia (tokenIdempotencia),
    KEY idx_reserva_orden (idOrdenOnline),
    KEY idx_reserva_estado_expira (estado, fechaExpiracion),
    KEY idx_reserva_cliente (idCliente),
    KEY idx_reserva_sede (idSede),
    CONSTRAINT fk_reserva_orden
        FOREIGN KEY (idOrdenOnline) REFERENCES orden_compra_online(idOrdenOnline),
    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (idCliente) REFERENCES clientes(idClientes),
    CONSTRAINT fk_reserva_sede
        FOREIGN KEY (idSede) REFERENCES sedes(idSede)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* =========================================================
   4. CREAR TABLA inventario_reserva_detalle
   ========================================================= */

CREATE TABLE inventario_reserva_detalle (
    idReservaDetalle INT NOT NULL AUTO_INCREMENT,
    idReserva INT NOT NULL,
    idOrdenDetalle INT NOT NULL,
    idProducto INT NOT NULL,
    idPrecioPresentacion INT NOT NULL,
    idUnidad INT NULL,
    nombreProducto VARCHAR(120) NOT NULL,
    nombrePresentacion VARCHAR(100) NOT NULL,
    tipoCalculoVenta VARCHAR(30) NOT NULL,
    factorConversionStock DECIMAL(18,6) NOT NULL,
    cantidadSolicitada DECIMAL(18,6) NOT NULL,
    cantidadReservadaStock DECIMAL(18,6) NOT NULL,
    PRIMARY KEY (idReservaDetalle),
    KEY idx_reserva_detalle_reserva (idReserva),
    KEY idx_reserva_detalle_orden_detalle (idOrdenDetalle),
    KEY idx_reserva_detalle_producto (idProducto),
    KEY idx_reserva_detalle_presentacion (idPrecioPresentacion),
    CONSTRAINT fk_reserva_detalle_reserva
        FOREIGN KEY (idReserva) REFERENCES inventario_reserva(idReserva),
    CONSTRAINT fk_reserva_detalle_orden_detalle
        FOREIGN KEY (idOrdenDetalle) REFERENCES orden_compra_online_detalle(idOrdenDetalle),
    CONSTRAINT fk_reserva_detalle_producto
        FOREIGN KEY (idProducto) REFERENCES productos(idProducto),
    CONSTRAINT fk_reserva_detalle_presentacion
        FOREIGN KEY (idPrecioPresentacion) REFERENCES producto_precio_presentacion(idPrecioPresentacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* =========================================================
   5. ALTER pagos_online
   ========================================================= */

ALTER TABLE pagos_online
    MODIFY COLUMN idVenta INT NULL,
    MODIFY COLUMN fechaPago DATETIME NULL DEFAULT NULL,
    ADD COLUMN idOrdenOnline INT NULL,
    ADD COLUMN idReserva INT NULL,
    ADD COLUMN idCliente INT NULL,
    ADD COLUMN idSede INT NULL,
    ADD COLUMN codigoPago VARCHAR(40) NULL,
    ADD COLUMN ambiente VARCHAR(20) NOT NULL DEFAULT 'TEST',
    ADD COLUMN estadoInterno VARCHAR(30) NULL,
    ADD COLUMN fechaCreacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN fechaActualizacion DATETIME NULL,
    ADD COLUMN fechaExpiracion DATETIME NULL,
    ADD COLUMN tokenIdempotencia VARCHAR(120) NULL,
    ADD COLUMN intentosConfirmacion INT NOT NULL DEFAULT 0,
    ADD COLUMN mensajeError VARCHAR(255) NULL;

ALTER TABLE pagos_online
    ADD UNIQUE KEY uk_pago_codigo (codigoPago),
    ADD UNIQUE KEY uk_pago_idempotencia (tokenIdempotencia),
    ADD UNIQUE KEY uk_pago_transaccion (idTransaccion),
    ADD KEY idx_pago_orden (idOrdenOnline),
    ADD KEY idx_pago_reserva (idReserva),
    ADD KEY idx_pago_estado (estadoPago),
    ADD KEY idx_pago_cliente (idCliente),
    ADD KEY idx_pago_sede (idSede);

ALTER TABLE pagos_online
    ADD CONSTRAINT fk_pago_orden
        FOREIGN KEY (idOrdenOnline) REFERENCES orden_compra_online(idOrdenOnline),
    ADD CONSTRAINT fk_pago_reserva
        FOREIGN KEY (idReserva) REFERENCES inventario_reserva(idReserva),
    ADD CONSTRAINT fk_pago_cliente
        FOREIGN KEY (idCliente) REFERENCES clientes(idClientes),
    ADD CONSTRAINT fk_pago_sede
        FOREIGN KEY (idSede) REFERENCES sedes(idSede);

/* =========================================================
   6. ALTER ventas
   ========================================================= */

ALTER TABLE ventas
    ADD COLUMN idOrdenOnline INT NULL,
    ADD UNIQUE KEY uk_ventas_orden_online (idOrdenOnline),
    ADD CONSTRAINT fk_ventas_orden_online
        FOREIGN KEY (idOrdenOnline) REFERENCES orden_compra_online(idOrdenOnline);

/* =========================================================
   7. ALTER pedidos
   ========================================================= */

ALTER TABLE pedidos
    ADD COLUMN idOrdenOnline INT NULL,
    ADD COLUMN idHorarioRecojo INT NULL,
    ADD UNIQUE KEY uk_pedidos_orden_online (idOrdenOnline),
    ADD KEY idx_pedidos_horario_recojo (idHorarioRecojo),
    ADD CONSTRAINT fk_pedidos_orden_online
        FOREIGN KEY (idOrdenOnline) REFERENCES orden_compra_online(idOrdenOnline),
    ADD CONSTRAINT fk_pedidos_horario_recojo
        FOREIGN KEY (idHorarioRecojo) REFERENCES sede_horario_recojo(idHorarioRecojo);

/* =========================================================
   8. ALTER detalleventa
   ========================================================= */

ALTER TABLE detalleventa
    ADD COLUMN precioNormal DECIMAL(10,2) NULL,
    ADD COLUMN precioOferta DECIMAL(10,2) NULL,
    ADD COLUMN precioFinalOnline DECIMAL(10,2) NULL,
    ADD COLUMN descuentoOferta DECIMAL(10,2) NULL,
    ADD COLUMN idOferta INT NULL,
    ADD KEY idx_detalleventa_oferta (idOferta),
    ADD CONSTRAINT fk_detalleventa_oferta
        FOREIGN KEY (idOferta) REFERENCES producto_presentacion_oferta(idOferta);

/* =========================================================
   9. ALTER detallepedido
   ========================================================= */

ALTER TABLE detallepedido
    ADD COLUMN idPrecioPresentacion INT NULL,
    ADD COLUMN nombrePresentacion VARCHAR(100) NULL,
    ADD COLUMN factorConversionStock DECIMAL(18,6) NULL,
    ADD COLUMN cantidadDescontadaStock DECIMAL(18,6) NULL,
    ADD COLUMN precioNormal DECIMAL(10,2) NULL,
    ADD COLUMN precioOferta DECIMAL(10,2) NULL,
    ADD COLUMN precioFinalOnline DECIMAL(10,2) NULL,
    ADD COLUMN descuentoOferta DECIMAL(10,2) NULL,
    ADD COLUMN idOferta INT NULL,
    ADD KEY idx_detallepedido_precio_presentacion (idPrecioPresentacion),
    ADD KEY idx_detallepedido_oferta (idOferta),
    ADD CONSTRAINT fk_detallepedido_precio_presentacion
        FOREIGN KEY (idPrecioPresentacion) REFERENCES producto_precio_presentacion(idPrecioPresentacion),
    ADD CONSTRAINT fk_detallepedido_oferta
        FOREIGN KEY (idOferta) REFERENCES producto_presentacion_oferta(idOferta);
