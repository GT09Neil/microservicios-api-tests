package com.uq.apitests.steps;

import com.github.javafaker.Faker;
import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

public class StepDefinitions {

    private static final Faker faker = new Faker();
    private static final String BASE_URL = "http://localhost:8080/api/v1";

    private String email;
    private String password;
    private String name;
    private String phone;
    private int userId;
    private String token;
    private int otp;

    private Response lastResponse;

    // --- Steps ---

    @Given("que genero un nuevo usuario aleatorio")
    public void genero_usuario_aleatorio() {
        this.email = faker.internet().emailAddress();
        this.password = "Passw0rd" + faker.number().digits(3);
        this.name = faker.name().fullName();
        this.phone = faker.phoneNumber().cellPhone();
    }

    @When("registro el usuario")
    public void registro_usuario() {
        Map<String, Object> body = new HashMap<>();
        body.put("email", this.email);
        body.put("password", this.password);
        body.put("name", this.name);
        body.put("phone", this.phone);

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/users");

        lastResponse.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("com/uq/apitests/user-response-schema.json"));

        this.userId = lastResponse.jsonPath().getInt("id");
    }

    @Then("recibo el enlace de activación")
    public void recibe_enlace_activacion() {
        // simulación, sin output
    }

    @When("activo la cuenta del usuario")
    public void activo_cuenta_usuario() {
        lastResponse = RestAssured.given()
                .patch(BASE_URL + "/users/" + this.userId + "/account_status");

        lastResponse.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("com/uq/apitests/account-status-response-schema.json"))
                .body("account_status", equalTo("VERIFIED"));
    }

    @When("inicio sesión con el usuario")
    public void login_usuario() {
        Map<String, Object> body = new HashMap<>();
        body.put("email", this.email);
        body.put("password", this.password);

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/auth/login");

        lastResponse.then().statusCode(200);

        this.token = lastResponse.getBody().asString();
    }

    @When("solicito un OTP para cambiar la contraseña")
    public void solicito_otp() {
        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", this.email))
                .post(BASE_URL + "/auth/otp");

        lastResponse.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("com/uq/apitests/otp-response-schema.json"));

        this.otp = lastResponse.jsonPath().getInt("otp");
    }

    @When("cambio la contraseña del usuario")
    public void cambio_contraseña() {
        String newPassword = "NewPassw0rd" + faker.number().digits(3);
        Map<String, Object> body = new HashMap<>();
        body.put("email", this.email);
        body.put("otp", this.otp);
        body.put("password", newPassword);

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .patch(BASE_URL + "/users/" + this.userId + "/password");

        lastResponse.then()
                .statusCode(200)
                .body(equalTo("Contraseña reestablecida para el usuario"));

        this.password = newPassword;
    }

    @When("actualizo los datos del usuario")
    public void actualizo_datos_usuario() {
        String newPhone = faker.phoneNumber().cellPhone();
        String newName = this.name + " Modificado";
        String newEmail = "modificado." + this.email;
        Map<String, Object> body = new HashMap<>();
        body.put("name", newName);
        body.put("email", newEmail);
        body.put("phone", newPhone);

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + this.token)
                .body(body)
                .put(BASE_URL + "/users/" + this.userId);

        lastResponse.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("com/uq/apitests/user-response-schema.json"))
                .body("name", equalTo(newName))
                .body("email", equalTo(newEmail))
                .body("phone", equalTo(newPhone));

        this.name = newName;
        this.phone = newPhone;
        this.email = newEmail;
    }

    @When("elimino el usuario")
    public void elimino_usuario() {
        lastResponse = RestAssured.given()
                .header("Authorization", "Bearer " + this.token)
                .delete(BASE_URL + "/users/" + this.userId);

        lastResponse.then().statusCode(204);
    }

    // --- Escenarios negativos ---

    @When("registro el mismo usuario nuevamente")
    public void registro_usuario_duplicado() {
        Map<String, Object> body = new HashMap<>();
        body.put("email", this.email);
        body.put("password", this.password);
        body.put("name", this.name);
        body.put("phone", faker.phoneNumber().cellPhone());

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/users");

        lastResponse.then()
                .statusCode(409)
                .body(matchesJsonSchemaInClasspath("com/uq/apitests/error-response-schema.json"));
    }

    @When("intento login con contraseña incorrecta")
    public void login_contrasena_incorrecta() {
        Map<String, Object> body = new HashMap<>();
        body.put("email", this.email);
        body.put("password", "Incorrecta123");

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/auth/login");

        lastResponse.then()
                .statusCode(400)
                .body(matchesJsonSchemaInClasspath("com/uq/apitests/error-response-schema.json"));
    }

    // --- Then / And genéricos ---

    @Then("el código de estado de respuesta debe ser {int}")
    public void el_código_de_estado_de_respuesta_debe_ser(Integer expectedStatus) {
        lastResponse.then().statusCode(expectedStatus);
    }

    @Then("la respuesta debe coincidir con el esquema {string}")
    public void la_respuesta_debe_coincidir_con_el_esquema(String schemaName) {
        lastResponse.then().body(matchesJsonSchemaInClasspath("com/uq/apitests/" + schemaName));
    }

    @Then("el OTP generado se almacena")
    public void el_otp_generado_se_almacena() {
        // sin output
    }

    @Then("la respuesta debe ser {string}")
    public void la_respuesta_debe_ser(String expectedBody) {
        lastResponse.then().body(equalTo(expectedBody));
    }
}
