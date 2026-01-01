package br.com.lunacom.ivmmonitorprocessamento.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;

@AllArgsConstructor
@Getter
public enum EscalaRecomendacao {
    RECOMENDACAO_01("1", "Recomendação 01"),
    RECOMENDACAO_02("2", "Recomendação 02"),
    RECOMENDACAO_03("3", "Recomendação 03"),
    RECOMENDACAO_04("4", "Recomendação 04"),
    RECOMENDACAO_05("5", "Recomendação 05"),
    RECOMENDACAO_06("6", "Recomendação 06"),
    RECOMENDACAO_07("7", "Recomendação 07"),
    RECOMENDACAO_08("8", "Recomendação 08"),
    RECOMENDACAO_09("9", "Recomendação 09"),
    RECOMENDACAO_10("10", "Recomendação 10");

    private final String codigo;
    private final String descricao;

    public static EscalaRecomendacao fromCodigo(String value) {
        return EnumSet.allOf(EscalaRecomendacao.class)
                .stream()
                .filter(it -> it.getCodigo().equals(value))
                .findFirst()
                .orElse(RECOMENDACAO_01);
    }
}
