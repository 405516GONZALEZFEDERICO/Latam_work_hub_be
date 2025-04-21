-- ROLES
INSERT INTO ROLES (name) VALUES
                             ('ADMIN'),
                             ('DEFAULT'),
                             ('PROVEEDOR'),
                             ('CLIENTE');

-- Inserción de países de Latinoamérica
INSERT INTO PAISES (name) VALUES
                              ('Argentina'),
                              ('Bolivia'),
                              ('Brasil'),
                              ('Chile'),
                              ('Colombia'),
                              ('Costa Rica'),
                              ('Cuba'),
                              ('Ecuador'),
                              ('El Salvador'),
                              ('Guatemala'),
                              ('Honduras'),
                              ('México'),
                              ('Nicaragua'),
                              ('Panamá'),
                              ('Paraguay'),
                              ('Perú'),
                              ('República Dominicana'),
                              ('Uruguay'),
                              ('Venezuela');

-- Ciudades de Argentina con provincias
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Buenos Aires', 'Buenos Aires', 'PROVINCE', 1),
                                                                          ('Córdoba', 'Córdoba', 'PROVINCE', 1),
                                                                          ('Rosario', 'Santa Fe', 'PROVINCE', 1),
                                                                          ('Mendoza', 'Mendoza', 'PROVINCE', 1),
                                                                          ('Tucumán', 'Tucumán', 'PROVINCE', 1),
                                                                          ('La Plata', 'Buenos Aires', 'PROVINCE', 1),
                                                                          ('Mar del Plata', 'Buenos Aires', 'PROVINCE', 1),
                                                                          ('Salta', 'Salta', 'PROVINCE', 1),
                                                                          ('Santa Fe', 'Santa Fe', 'PROVINCE', 1),
                                                                          ('San Juan', 'San Juan', 'PROVINCE', 1);

-- Ciudades de Bolivia con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('La Paz', 'La Paz', 'DEPARTMENT', 2),
                                                                          ('Santa Cruz de la Sierra', 'Santa Cruz', 'DEPARTMENT', 2),
                                                                          ('Cochabamba', 'Cochabamba', 'DEPARTMENT', 2),
                                                                          ('Sucre', 'Chuquisaca', 'DEPARTMENT', 2),
                                                                          ('Tarija', 'Tarija', 'DEPARTMENT', 2);

-- Ciudades de Brasil con estados
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('São Paulo', 'São Paulo', 'STATE', 3),
                                                                          ('Río de Janeiro', 'Río de Janeiro', 'STATE', 3),
                                                                          ('Brasilia', 'Distrito Federal', 'STATE', 3),
                                                                          ('Salvador', 'Bahía', 'STATE', 3),
                                                                          ('Fortaleza', 'Ceará', 'STATE', 3),
                                                                          ('Belo Horizonte', 'Minas Gerais', 'STATE', 3),
                                                                          ('Manaus', 'Amazonas', 'STATE', 3),
                                                                          ('Curitiba', 'Paraná', 'STATE', 3),
                                                                          ('Recife', 'Pernambuco', 'STATE', 3),
                                                                          ('Porto Alegre', 'Rio Grande do Sul', 'STATE', 3);

-- Ciudades de Chile con regiones
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Santiago', 'Metropolitana', 'REGION', 4),
                                                                          ('Valparaíso', 'Valparaíso', 'REGION', 4),
                                                                          ('Concepción', 'Biobío', 'REGION', 4),
                                                                          ('Antofagasta', 'Antofagasta', 'REGION', 4),
                                                                          ('Viña del Mar', 'Valparaíso', 'REGION', 4),
                                                                          ('Talca', 'Maule', 'REGION', 4),
                                                                          ('Temuco', 'Araucanía', 'REGION', 4);

-- Ciudades de Colombia con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Bogotá', 'Cundinamarca', 'DEPARTMENT', 5),
                                                                          ('Medellín', 'Antioquia', 'DEPARTMENT', 5),
                                                                          ('Cali', 'Valle del Cauca', 'DEPARTMENT', 5),
                                                                          ('Barranquilla', 'Atlántico', 'DEPARTMENT', 5),
                                                                          ('Cartagena', 'Bolívar', 'DEPARTMENT', 5),
                                                                          ('Cúcuta', 'Norte de Santander', 'DEPARTMENT', 5),
                                                                          ('Bucaramanga', 'Santander', 'DEPARTMENT', 5);

-- Ciudades de Costa Rica con provincias
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('San José', 'San José', 'PROVINCE', 6),
                                                                          ('Alajuela', 'Alajuela', 'PROVINCE', 6),
                                                                          ('Cartago', 'Cartago', 'PROVINCE', 6),
                                                                          ('Heredia', 'Heredia', 'PROVINCE', 6),
                                                                          ('Liberia', 'Guanacaste', 'PROVINCE', 6),
                                                                          ('Puntarenas', 'Puntarenas', 'PROVINCE', 6),
                                                                          ('Limón', 'Limón', 'PROVINCE', 6);

-- Ciudades de Cuba con provincias
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('La Habana', 'La Habana', 'PROVINCE', 7),
                                                                          ('Santiago de Cuba', 'Santiago de Cuba', 'PROVINCE', 7),
                                                                          ('Camagüey', 'Camagüey', 'PROVINCE', 7),
                                                                          ('Holguín', 'Holguín', 'PROVINCE', 7),
                                                                          ('Santa Clara', 'Villa Clara', 'PROVINCE', 7),
                                                                          ('Guantánamo', 'Guantánamo', 'PROVINCE', 7),
                                                                          ('Matanzas', 'Matanzas', 'PROVINCE', 7),
                                                                          ('Cienfuegos', 'Cienfuegos', 'PROVINCE', 7);

-- Ciudades de Ecuador con provincias
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Quito', 'Pichincha', 'PROVINCE', 8),
                                                                          ('Guayaquil', 'Guayas', 'PROVINCE', 8),
                                                                          ('Cuenca', 'Azuay', 'PROVINCE', 8),
                                                                          ('Santo Domingo', 'Santo Domingo de los Tsáchilas', 'PROVINCE', 8),
                                                                          ('Ambato', 'Tungurahua', 'PROVINCE', 8);

-- Ciudades de El Salvador con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('San Salvador', 'San Salvador', 'DEPARTMENT', 9),
                                                                          ('Santa Ana', 'Santa Ana', 'DEPARTMENT', 9),
                                                                          ('San Miguel', 'San Miguel', 'DEPARTMENT', 9),
                                                                          ('Santa Tecla', 'La Libertad', 'DEPARTMENT', 9),
                                                                          ('Soyapango', 'San Salvador', 'DEPARTMENT', 9),
                                                                          ('Mejicanos', 'San Salvador', 'DEPARTMENT', 9);

-- Ciudades de Guatemala con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Ciudad de Guatemala', 'Guatemala', 'DEPARTMENT', 10),
                                                                          ('Quetzaltenango', 'Quetzaltenango', 'DEPARTMENT', 10),
                                                                          ('Escuintla', 'Escuintla', 'DEPARTMENT', 10),
                                                                          ('Mixco', 'Guatemala', 'DEPARTMENT', 10),
                                                                          ('Villa Nueva', 'Guatemala', 'DEPARTMENT', 10),
                                                                          ('Cobán', 'Alta Verapaz', 'DEPARTMENT', 10),
                                                                          ('Huehuetenango', 'Huehuetenango', 'DEPARTMENT', 10);

-- Ciudades de Honduras con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Tegucigalpa', 'Francisco Morazán', 'DEPARTMENT', 11),
                                                                          ('San Pedro Sula', 'Cortés', 'DEPARTMENT', 11),
                                                                          ('La Ceiba', 'Atlántida', 'DEPARTMENT', 11),
                                                                          ('Choloma', 'Cortés', 'DEPARTMENT', 11),
                                                                          ('El Progreso', 'Yoro', 'DEPARTMENT', 11),
                                                                          ('Choluteca', 'Choluteca', 'DEPARTMENT', 11);

-- Ciudades de México con estados
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Ciudad de México', 'Ciudad de México', 'STATE', 12),
                                                                          ('Guadalajara', 'Jalisco', 'STATE', 12),
                                                                          ('Monterrey', 'Nuevo León', 'STATE', 12),
                                                                          ('Puebla', 'Puebla', 'STATE', 12),
                                                                          ('Tijuana', 'Baja California', 'STATE', 12),
                                                                          ('León', 'Guanajuato', 'STATE', 12),
                                                                          ('Juárez', 'Chihuahua', 'STATE', 12),
                                                                          ('Querétaro', 'Querétaro', 'STATE', 12);

-- Ciudades de Nicaragua con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Managua', 'Managua', 'DEPARTMENT', 13),
                                                                          ('León', 'León', 'DEPARTMENT', 13),
                                                                          ('Masaya', 'Masaya', 'DEPARTMENT', 13),
                                                                          ('Chinandega', 'Chinandega', 'DEPARTMENT', 13),
                                                                          ('Matagalpa', 'Matagalpa', 'DEPARTMENT', 13),
                                                                          ('Granada', 'Granada', 'DEPARTMENT', 13),
                                                                          ('Estelí', 'Estelí', 'DEPARTMENT', 13);

-- Ciudades de Panamá con provincias
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Ciudad de Panamá', 'Panamá', 'PROVINCE', 14),
                                                                          ('San Miguelito', 'Panamá', 'PROVINCE', 14),
                                                                          ('David', 'Chiriquí', 'PROVINCE', 14),
                                                                          ('Colón', 'Colón', 'PROVINCE', 14),
                                                                          ('Santiago', 'Veraguas', 'PROVINCE', 14),
                                                                          ('Arraiján', 'Panamá Oeste', 'PROVINCE', 14),
                                                                          ('La Chorrera', 'Panamá Oeste', 'PROVINCE', 14);

-- Ciudades de Paraguay con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Asunción', 'Asunción', 'DEPARTMENT', 15),
                                                                          ('Ciudad del Este', 'Alto Paraná', 'DEPARTMENT', 15),
                                                                          ('Encarnación', 'Itapúa', 'DEPARTMENT', 15),
                                                                          ('San Lorenzo', 'Central', 'DEPARTMENT', 15);

-- Ciudades de Perú con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Lima', 'Lima', 'DEPARTMENT', 16),
                                                                          ('Arequipa', 'Arequipa', 'DEPARTMENT', 16),
                                                                          ('Trujillo', 'La Libertad', 'DEPARTMENT', 16),
                                                                          ('Chiclayo', 'Lambayeque', 'DEPARTMENT', 16),
                                                                          ('Cusco', 'Cusco', 'DEPARTMENT', 16),
                                                                          ('Piura', 'Piura', 'DEPARTMENT', 16);

-- Ciudades de República Dominicana con provincias
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Santo Domingo', 'Distrito Nacional', 'PROVINCE', 17),
                                                                          ('Santiago de los Caballeros', 'Santiago', 'PROVINCE', 17),
                                                                          ('San Pedro de Macorís', 'San Pedro de Macorís', 'PROVINCE', 17),
                                                                          ('La Romana', 'La Romana', 'PROVINCE', 17),
                                                                          ('San Francisco de Macorís', 'Duarte', 'PROVINCE', 17),
                                                                          ('Puerto Plata', 'Puerto Plata', 'PROVINCE', 17),
                                                                          ('Higüey', 'La Altagracia', 'PROVINCE', 17),
                                                                          ('San Cristóbal', 'San Cristóbal', 'PROVINCE', 17);

-- Ciudades de Uruguay con departamentos
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Montevideo', 'Montevideo', 'DEPARTMENT', 18),
                                                                          ('Salto', 'Salto', 'DEPARTMENT', 18),
                                                                          ('Paysandú', 'Paysandú', 'DEPARTMENT', 18),
                                                                          ('Las Piedras', 'Canelones', 'DEPARTMENT', 18),
                                                                          ('Rivera', 'Rivera', 'DEPARTMENT', 18);

-- Ciudades de Venezuela con estados
INSERT INTO CIUDADES (name, division_name, division_type, country_id) VALUES
                                                                          ('Caracas', 'Distrito Capital', 'STATE', 19),
                                                                          ('Maracaibo', 'Zulia', 'STATE', 19),
                                                                          ('Valencia', 'Carabobo', 'STATE', 19),
                                                                          ('Barquisimeto', 'Lara', 'STATE', 19),
                                                                          ('Maracay', 'Aragua', 'STATE', 19);