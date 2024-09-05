package helen.alves;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.specification.FilterableRequestSpecification;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Epic("Testes de API REST usando JUnit e Rest-Assured")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BarrigaTest extends BaseTest {

        private static String CONTA_NAME = "Conta " + System.nanoTime();
        private static Integer CONTA_ID;
        private static Integer MOV_ID;

        @BeforeClass
        public static void login(){
            Map<String, String> login = new HashMap<>();
            login.put("email", "helen@gmail.com");
            login.put("senha", "12345");
            String TOKEN = given()
                    .body(login)
                    .when()
                    .post("/signin")
                    .then()
                    .statusCode(200)
                    .extract().path("token")
                    ;
            requestSpecification.header("Authorization",  "JWT " + TOKEN);

            get("/reset").then().statusCode(200);
        }

        @Test
        @Story("Incluir Conta")
        @Description("Este teste verifica a inclusão de uma conta com sucesso.")
        public void t01_deveIncluirContaComSucesso() {
            Map<String, String> conta = new HashMap<>();
            conta.put("nome", CONTA_NAME);
            CONTA_ID = given()
                    .filter(new AllureRestAssured())
                    .body(conta)
                    .when()
                    .post("/contas")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().path("id");
        }

        @Test
        @Story("Alterar Conta")
        @Description("Este teste verifica a alteração de uma conta com sucesso.")
        public void t02_deveAlterarContaComSucesso() {
            Map<String, String> conta = new HashMap<>();
            conta.put("nome", CONTA_NAME + "alterada");
            given()
                    .filter(new AllureRestAssured())
                    .body(conta)
                    .pathParam("id", CONTA_ID)
                    .when()
                    .put("/contas/{id}")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("nome", is(CONTA_NAME + "alterada"))
            ;

        }

        @Test
        @Story("Verificar Conta Duplicada")
        @Description("Este teste garante que nomes de contas duplicados não são aceitos.")
        public void t03_naoDeveAceitarNomesRepetidos() {
            Map<String, String> conta = new HashMap<>();
            conta.put("nome", CONTA_NAME + "alterada");
            given()
                    .body(conta)
                    .filter(new AllureRestAssured())
                    .when()
                    .post("/contas")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .body("error", is("Já existe uma conta com esse nome!"));

        }

        @Test
        @Story("Inserir Movimentação")
        @Description("Este teste verifica a inserção de uma movimentação com sucesso.")
        public void t04_deveInserirMovimentacaoComSucesso() {
            Movimentacao mov = getMovimentacaoValida();
            MOV_ID = given()
                    .body(mov)
                    .filter(new AllureRestAssured())
                    .when()
                    .post("/transacoes")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().path("id");
        }

        @Test
        @Story("Validar Campos Obrigatórios")
        @Description("Este teste valida a presença dos campos obrigatórios em uma movimentação.")
        public void t05_deveValidaarCamposObrigatoriosMovimentacao() {
            Movimentacao movimentacao2 = new Movimentacao();
            given()
                    .filter(new AllureRestAssured())
                    .body(movimentacao2)
                    .when()
                    .filter(new AllureRestAssured())
                    .post("/transacoes")
                    .then()
                    .log().all()
                    .statusCode(400);

        }

        @Test
        @Story("Movimentação Futura")
        @Description("Este teste garante que não é possível inserir uma movimentação futura.")
        public void t06_naoDeveInserirTransacaoFutura() {
            Movimentacao mov = getMovimentacaoValida();
            mov.setData_transacao(DataUtils.getDataDiferencaDIas(2));

            given()
                    .filter(new AllureRestAssured())
                    .body(mov)
                    .when()
                    .post("/transacoes")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .body("$", hasSize(1))
                    .body("msg", hasItem("Data da Movimentação deve ser menor ou igual à data atual"));

        }

        @Test
        @Story("Remover Conta com Movimentação")
        @Description("Este teste garante que não é possível remover uma conta que tenha movimentações associadas.")
        public void t07_naoDeveRemoverContaComMovimentacao() {
            given()
                    .filter(new AllureRestAssured())
                    .pathParam("id", CONTA_ID)
                    .when()
                    .delete("/contas/{id}}")
                    .then()
                    .log().all()
                    .statusCode(500);
        }

        @Test
        @Story("Calcular Saldo das Contas")
        @Description("Este teste verifica o cálculo do saldo das contas.")
        public void t08_deveCalcularSaldoContas() {
            given()
                    .filter(new AllureRestAssured())
                    .when()
                    .get("/saldo")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("saldo", is(notNullValue()))
            ;
        }

        @Test
        @Story("Remover Movimentação")
        @Description("Este teste verifica a remoção de uma movimentação com sucesso.")
        public void t09_deveRemoverMovimentacao() {
            given()
                    .filter(new AllureRestAssured())
                    .pathParam("id", MOV_ID)
                    .when()
                    .delete("/transacoes/{id}")
                    .then()
                    .log().all()
                    .statusCode(204)
            ;
        }

        @Test
        @Story("Acesso sem Token")
        @Description("Este teste garante que não é possível acessar informações sem um token válido.")
        public void t10_naoDeveAcessarSemToken() {
            FilterableRequestSpecification req = (FilterableRequestSpecification) requestSpecification;
            req.removeHeader("Authorization");

            given()
                    .filter(new AllureRestAssured())
                    .when()
                    .get("/contas")
                    .then()
                    .log().all()
                    .statusCode(401);
        }

        private Movimentacao getMovimentacaoValida(){
            Movimentacao mov = new Movimentacao();
            mov.setConta_id(CONTA_ID );
            mov.setDescricao("Segunda transação");
            mov.setEnvolvido("Joana");
            mov.setTipo("REC");
            mov.setData_pagamento(DataUtils.getDataDiferencaDIas(-3));
            mov.setData_transacao(DataUtils.getDataDiferencaDIas(-2));
            mov.setValor(1200.00f);
            mov.setStatus(true);

            return mov;
        }
    }
