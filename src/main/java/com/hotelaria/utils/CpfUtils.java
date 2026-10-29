package com.hotelaria.utils;

/**
 * Funções auxiliares para tratamento de CPF. A validação dos dígitos verificadores
 * é feita pela anotação {@code @CPF} do Hibernate Validator nos DTOs.
 */
public final class CpfUtils {

    private CpfUtils() {
    }

    /** Remove pontuação, mantendo apenas os dígitos. */
    public static String normalize(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /** Indica se o login informado deve ser tratado como CPF (11 dígitos, sem "@"). */
    public static boolean looksLikeCpf(String login) {
        return login != null && !login.contains("@") && normalize(login).length() == 11;
    }

    /** Mascara o CPF para exibição em listagens: ***.456.789-** */
    public static String mask(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "***";
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
