package financeiro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import financeiro.service.CadastrosService;

/**
 * Controller de movimentações.
 *
 * Fase atual: apenas os endpoints de consulta dos cadastros-base (categorias e
 * formas de pagamento) necessários para popular os formulários de lançamento.
 *
 * O CRUD de movimentações será implementado na próxima etapa.
 */
@Controller
@RequestMapping("/movimentacoes")
public class MovimentacaoController {

    private final CadastrosService cadastrosService;

    public MovimentacaoController(CadastrosService cadastrosService) {
        this.cadastrosService = cadastrosService;
    }

    // -------------------------------------------------------------------------
    // Tela principal de movimentações
    // -------------------------------------------------------------------------

    @GetMapping
    public String movimentacoes() {
        return "movimentacoes";
    }

    // -------------------------------------------------------------------------
    // APIs de suporte aos formulários — retornam JSON
    // -------------------------------------------------------------------------

    /**
     * GET /movimentacoes/categorias
     *
     * Retorna a lista de categorias de movimentação ativas, ordenadas por nome.
     * Usado pelos formulários de novo lançamento para popular o select de categoria.
     * Requer autenticação (protegido pelo SecurityConfig).
     */
    @GetMapping("/categorias")
    @ResponseBody
    public ResponseEntity<?> listarCategorias() {
        return ResponseEntity.ok(cadastrosService.listarCategorias());
    }

    /**
     * GET /movimentacoes/formas-pagamento
     *
     * Retorna a lista de formas de pagamento ativas, ordenadas por nome.
     * Usado pelos formulários de novo lançamento para popular o select de forma de pagamento.
     * O campo id_forma_pagamento é nullable em movimentacao, portanto este endpoint
     * pode retornar lista vazia sem impedir o lançamento.
     * Requer autenticação (protegido pelo SecurityConfig).
     */
    @GetMapping("/formas-pagamento")
    @ResponseBody
    public ResponseEntity<?> listarFormasPagamento() {
        return ResponseEntity.ok(cadastrosService.listarFormasPagamento());
    }
}
