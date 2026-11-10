package financeiro.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import financeiro.repository.InstituicaoRepository;
import financeiro.repository.InstituicaoRepository.Instituicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

class PixServiceTests {

    private final InstituicaoRepository repository = mock(InstituicaoRepository.class);
    private final PixService service = new PixService(repository);

    @Test
    void carregaChaveExistente() {
        when(repository.consultar(false)).thenReturn(List.of(new Instituicao(7, "contato@example.org")));
        assertEquals("contato@example.org", service.consultar().chave());
    }

    @Test
    void chaveAusenteOuInvalidaNaoHabilitaCopia() {
        for (String chave : new String[]{null, "", "  ", "chave-invalida", "11111111111"}) {
            when(repository.consultar(false)).thenReturn(List.of(new Instituicao(7, chave)));
            assertFalse(service.consultar().disponivel());
        }
    }

    @Test
    void naoEscolheInstituicaoAusenteOuAmbigua() {
        when(repository.consultar(false)).thenReturn(List.of());
        assertFalse(service.consultar().disponivel());
        when(repository.consultar(false)).thenReturn(List.of(new Instituicao(7, "a@example.org"),
                new Instituicao(8, "b@example.org")));
        assertFalse(service.consultar().disponivel());
    }

    @Test
    void atualizaSomenteCadastroExistenteComChaveNormalizada() {
        when(repository.consultar(true)).thenReturn(List.of(new Instituicao(7, null)));
        when(repository.atualizarChavePix(7, "contato@example.org")).thenReturn(1);
        assertEquals("contato@example.org", service.salvar("  contato@example.org  ").chave());
        verify(repository).atualizarChavePix(7, "contato@example.org");
    }

    @Test
    void rejeitaValoresInvalidosAntesDeAcessarBanco() {
        for (String chave : new String[]{null, "", "invalida", "11111111111", "12345678901",
                "12345678901234", "a".repeat(101), "a@example.org\nX"}) {
            assertThrows(IllegalArgumentException.class, () -> service.salvar(chave));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void aceitaFormatosDeChavePix() {
        when(repository.consultar(true)).thenReturn(List.of(new Instituicao(7, null)));
        when(repository.atualizarChavePix(anyInt(), anyString())).thenReturn(1);
        for (String chave : new String[]{"52998224725", "11222333000181", "+5511999999999",
                "contato@example.org", "123e4567-e89b-42d3-a456-426614174000"}) {
            assertEquals(chave, service.salvar(chave).chave());
        }
    }

    @Test
    void naoCriaInstituicaoNemAtualizaCadastroAmbiguo() {
        when(repository.consultar(true)).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> service.salvar("contato@example.org"));
        when(repository.consultar(true)).thenReturn(List.of(new Instituicao(7, null), new Instituicao(8, null)));
        assertThrows(IllegalStateException.class, () -> service.salvar("contato@example.org"));
        verify(repository, never()).atualizarChavePix(anyInt(), anyString());
    }
}
