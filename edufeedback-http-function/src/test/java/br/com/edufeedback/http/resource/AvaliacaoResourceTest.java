package br.com.edufeedback.http.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AvaliacaoResourceTest {

  @Test
  void deveRejeitarNotaInvalidaComoProblemDetails() {
    given()
        .contentType("application/json")
        .body("{\"descricao\":\"Descrição suficientemente longa\",\"nota\":11}")
        .when()
        .post("/api/v1/feedbacks")
        .then()
        .statusCode(400)
        .contentType(containsString("application/problem+json"))
        .body("type", equalTo("about:blank"))
        .body("title", equalTo("Dados inválidos"))
        .body("status", equalTo(400))
        .body("violations.nota", equalTo("deve ser menor que ou igual a 10"));
  }

  @Test
  void deveRejeitarJsonMalformadoComoProblemDetails() {
    given()
        .contentType("application/json")
        .body("{\"descricao\":\"JSON quebrado\",\"nota\":5")
        .when()
        .post("/api/v1/feedbacks")
        .then()
        .statusCode(400)
        .contentType(containsString("application/problem+json"))
        .body("title", equalTo("JSON inválido"))
        .body("status", equalTo(400));
  }

  @Test
  void deveRejeitarCorpoVazioComoProblemDetails() {
    given()
        .contentType("application/json")
        .body("")
        .when()
        .post("/api/v1/feedbacks")
        .then()
        .statusCode(400)
        .contentType(containsString("application/problem+json"))
        .body("status", equalTo(400));
  }

  @Test
  void deveExigirTokenParaListagem() {
    given().when().get("/api/v1/feedbacks").then().statusCode(401);
  }

  @Test
  void deveRealizarLoginAdministrativo() {
    given()
        .contentType("application/json")
        .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .body("tokenType", equalTo("Bearer"))
        .body("expiresIn", equalTo(3600));
  }
}
