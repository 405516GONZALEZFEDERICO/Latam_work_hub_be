-- PERMISOS (IDs manuales para facilitar relación)
INSERT INTO permissions (id, name) VALUES
                                       (1, 'CREAR_USUARIO'),
                                       (2, 'ELIMINAR_USUARIO'),
                                       (3, 'MODIFICAR_USUARIO'),
                                       (4, 'APROBAR_PROVEEDOR'),
                                       (5, 'ACTIVAR_CUENTA'),
                                       (6, 'MODIFICAR_ESPACIO'),
                                       (7, 'VER_TRANSACCIONES'),
                                       (8, 'CANCELAR_RESERVA'),
                                       (9, 'GESTIONAR_REEMBOLSOS'),
                                       (10, 'VER_INFORMES'),
                                       (11, 'CONFIGURAR_SISTEMA'),
                                       (12, 'CREAR_ESPACIO'),
                                       (13, 'ESTABLECER_PRECIO'),
                                       (14, 'GESTIONAR_CONTRATO'),
                                       (15, 'VER_PAGOS_RECIBIDOS'),
                                       (16, 'EMITIR_FACTURAS'),
                                       (17, 'VER_INFORMES_PROPIO'),
                                       (18, 'BUSCAR_ESPACIOS'),
                                       (19, 'VER_DETALLES_ESPACIO'),
                                       (20, 'CREAR_RESERVA'),
                                       (21, 'CANCELAR_RESERVA_CLIENTE'),
                                       (22, 'CALIFICAR_ESPACIO'),
                                       (23, 'REALIZAR_PAGO'),
                                       (24, 'VER_HISTORIAL_PAGOS'),
                                       (25, 'DESCARGAR_FACTURA'),
                                       (26, 'VER_ESPACIOS');

-- ROLES
INSERT INTO roles (id, name) VALUES
                                 (1, 'ADMIN'),
                                 (2, 'PROVEEDOR'),
                                 (3, 'CLIENTE');

-- PERMISOS DEL ROL ADMIN (todos los permisos)
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11),
                                                          (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21),
                                                          (1, 22), (1, 23), (1, 24), (1, 25), (1, 26);

-- PERMISOS DEL ROL PROVEEDOR
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (2, 12), -- CREAR_ESPACIO
                                                          (2, 13), -- ESTABLECER_PRECIO
                                                          (2, 14), -- GESTIONAR_CONTRATO
                                                          (2, 15), -- VER_PAGOS_RECIBIDOS
                                                          (2, 16), -- EMITIR_FACTURAS
                                                          (2, 17), -- VER_INFORMES_PROPIO
                                                          (2, 6),  -- MODIFICAR_ESPACIO
                                                          (2, 7),  -- VER_TRANSACCIONES
                                                          (2, 9),  -- GESTIONAR_REEMBOLSOS
                                                          (2, 10), -- VER_INFORMES
                                                          (2, 8),  -- CANCELAR_RESERVA

-- PERMISOS DEL ROL CLIENTE
                                                          (3, 18), -- BUSCAR_ESPACIOS
                                                          (3, 19), -- VER_DETALLES_ESPACIO
                                                          (3, 20), -- CREAR_RESERVA
                                                          (3, 21), -- CANCELAR_RESERVA_CLIENTE
                                                          (3, 22), -- CALIFICAR_ESPACIO
                                                          (3, 23), -- REALIZAR_PAGO
                                                          (3, 24), -- VER_HISTORIAL_PAGOS
                                                          (3, 25), -- DESCARGAR_FACTURA
                                                          (3, 26); -- VER_ESPACIOS
