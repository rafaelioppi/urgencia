package br.gov.saude.sgpur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protecao basica contra forca bruta no login: apos {@value #MAX_TENTATIVAS}
 * tentativas com senha errada para o mesmo usuario, bloqueia novas tentativas
 * por {@value #BLOQUEIO_MINUTOS} minutos (janela deslizante em memoria).
 *
 * <p>Nao substitui um WAF/rate-limit por IP, mas encarece um ataque de
 * dicionario contra um login especifico. Estado em memoria (nao persistido):
 * reinicia a contagem a cada restart da aplicacao - aceitavel para o volume
 * deste sistema (poucos usuarios, uso interno).
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private record Estado(int tentativas, Instant bloqueadoAte) {}

    private final ConcurrentHashMap<String, Estado> tentativasPorUsuario = new ConcurrentHashMap<>();

    /** True se o usuario esta temporariamente bloqueado por excesso de tentativas. */
    public boolean estaBloqueado(String username) {
        Estado e = tentativasPorUsuario.get(chave(username));
        return e != null && e.bloqueadoAte() != null && Instant.now().isBefore(e.bloqueadoAte());
    }

    @EventListener
    public void aoFalhar(AbstractAuthenticationFailureEvent evento) {
        String username = String.valueOf(evento.getAuthentication().getPrincipal());
        registrarFalha(username);
    }

    @EventListener
    public void aoLogarComSucesso(AuthenticationSuccessEvent evento) {
        tentativasPorUsuario.remove(chave(evento.getAuthentication().getName()));
    }

    private void registrarFalha(String username) {
        String k = chave(username);
        Estado atualizado = tentativasPorUsuario.compute(k, (key, atual) -> {
            int tentativas = (atual == null ? 0 : atual.tentativas()) + 1;
            Instant bloqueadoAte = tentativas >= MAX_TENTATIVAS
                ? Instant.now().plus(BLOQUEIO) : null;
            return new Estado(tentativas, bloqueadoAte);
        });
        if (atualizado.bloqueadoAte() != null) {
            log.warn("LoginAttemptService: usuario '{}' bloqueado por {} min apos {} tentativas.",
                username, BLOQUEIO.toMinutes(), atualizado.tentativas());
        }
    }

    private String chave(String username) {
        return username == null ? "" : username.toLowerCase(java.util.Locale.ROOT);
    }
}
