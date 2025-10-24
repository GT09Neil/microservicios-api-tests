Feature: Flujo completo de usuario
  In order to asegurar la correcta gestión de usuarios
  As tester
  I want to probar el flujo completo de registro, activación, login, actualización y eliminación, incluyendo casos negativos

  Scenario: Flujo completo de usuario
    Given que genero un nuevo usuario aleatorio
    When registro el usuario
    Then recibo el enlace de activación
    When activo la cuenta del usuario
    Then el código de estado de respuesta debe ser 200
    And la respuesta debe coincidir con el esquema "account-status-response-schema.json"

    When inicio sesión con el usuario
    Then el código de estado de respuesta debe ser 200

    When intento login con contraseña incorrecta
    Then el código de estado de respuesta debe ser 400
    And la respuesta debe coincidir con el esquema "error-response-schema.json"

    When registro el mismo usuario nuevamente
    Then el código de estado de respuesta debe ser 409

    When solicito un OTP para cambiar la contraseña
    Then el código de estado de respuesta debe ser 200
    And el OTP generado se almacena
    When cambio la contraseña del usuario
    Then el código de estado de respuesta debe ser 200
    And la respuesta debe ser "Contraseña reestablecida para el usuario"

    When actualizo los datos del usuario
    Then el código de estado de respuesta debe ser 200
    And la respuesta debe coincidir con el esquema "user-response-schema.json"

    When elimino el usuario
    Then el código de estado de respuesta debe ser 204
