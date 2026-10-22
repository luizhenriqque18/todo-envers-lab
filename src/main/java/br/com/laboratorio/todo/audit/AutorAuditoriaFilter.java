package br.com.laboratorio.todo.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AutorAuditoriaFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String autor = request.getHeader("X-Autor");
        if (autor != null && (autor.isBlank() || autor.length() > 100 || autor.chars().anyMatch(Character::isISOControl))) {
            response.sendError(400, "X-Autor deve ter entre 1 e 100 caracteres sem controles.");
            return;
        }
        // Identidade simulada para o laboratório. Em produção, usar o usuário autenticado.
        AutorAuditoria.definir(autor == null ? "anonimo-laboratorio" : autor.strip());
        try {
            chain.doFilter(request, response);
        } finally {
            AutorAuditoria.limpar();
        }
    }
}
