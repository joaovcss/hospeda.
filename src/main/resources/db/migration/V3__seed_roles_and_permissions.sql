INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'Administrador do hotel'),
    ('ROLE_EMPLOYEE', 'Funcionário do hotel'),
    ('ROLE_GUEST', 'Hóspede');

INSERT INTO permissions (name, description) VALUES
    ('USER_CREATE', 'Cadastrar usuários'),
    ('USER_READ', 'Consultar usuários'),
    ('USER_UPDATE', 'Atualizar dados de usuários'),
    ('USER_STATUS_UPDATE', 'Ativar ou desativar usuários'),
    ('PERMISSION_MANAGE', 'Conceder ou revogar permissões'),
    ('ROOM_CREATE', 'Cadastrar quartos'),
    ('ROOM_UPDATE', 'Atualizar quartos e seus status'),
    ('ROOM_READ', 'Consultar quartos'),
    ('AREA_CREATE', 'Cadastrar áreas do hotel'),
    ('AREA_UPDATE', 'Atualizar e desativar áreas do hotel'),
    ('AREA_READ', 'Consultar áreas do hotel'),
    ('GUEST_CREATE', 'Cadastrar hóspedes'),
    ('GUEST_READ', 'Consultar hóspedes'),
    ('GUEST_UPDATE', 'Atualizar hóspedes'),
    ('BOOKING_CREATE', 'Agendar hospedagens'),
    ('BOOKING_READ', 'Consultar hospedagens'),
    ('BOOKING_CONFIRM', 'Confirmar reservas'),
    ('BOOKING_CANCEL', 'Cancelar reservas'),
    ('CHECKIN', 'Realizar check-in'),
    ('CHECKOUT', 'Realizar check-out'),
    ('ACCESS_GRANT', 'Conceder acesso de hospedagens a áreas'),
    ('ACCESS_REVOKE', 'Revogar acesso de hospedagens a áreas'),
    ('ACCESS_READ', 'Consultar autorizações de acesso às áreas'),
    ('ACCESS_REGISTER', 'Registrar entrada de hóspedes nas áreas'),
    ('ACCESS_LOG_READ', 'Consultar o histórico de acessos às áreas');

-- Administrador: todas as permissões
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN';

-- Funcionário: permissões operacionais padrão
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'ROOM_READ', 'AREA_READ',
    'GUEST_CREATE', 'GUEST_READ', 'GUEST_UPDATE',
    'BOOKING_CREATE', 'BOOKING_READ', 'BOOKING_CONFIRM', 'BOOKING_CANCEL',
    'CHECKIN', 'CHECKOUT',
    'ACCESS_READ', 'ACCESS_REGISTER'
)
WHERE r.name = 'ROLE_EMPLOYEE';

-- Hóspede: apenas consulta das áreas do hotel (o restante é validado por propriedade)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN ('AREA_READ')
WHERE r.name = 'ROLE_GUEST';
