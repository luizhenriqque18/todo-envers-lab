package br.com.laboratorio.todo.audit;

public final class AutorAuditoria {
    private static final ThreadLocal<String> AUTOR = new ThreadLocal<>();

    private AutorAuditoria() {}

    public static String atual() {
        return AUTOR.get() == null ? "sistema" : AUTOR.get();
    }

    public static void definir(String autor) { AUTOR.set(autor); }
    public static void limpar() { AUTOR.remove(); }
}
