package br.com.lunacom.ivmmonitorprocessamento.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;

@AllArgsConstructor
@Getter
public enum Recomendacao {
    COMPRA("C", "Compra"),
    VENDA("V", "Venda"),
    NEUTRO("N", "Neutro");

    private final String codigo;
    private final String descricao;

    public static Recomendacao fromCodigo(String value) {
        return EnumSet.allOf(Recomendacao.class)
                .stream()
                .filter(it -> it.getCodigo().equals(value))
                .findFirst()
                .orElse(NEUTRO);
    }
}
