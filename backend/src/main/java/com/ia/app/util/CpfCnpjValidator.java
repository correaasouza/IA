package com.ia.app.util;

public class CpfCnpjValidator {
  private CpfCnpjValidator() {}

  public static boolean isValid(String value) {
    if (value == null) return false;
    String digits = value.replaceAll("\\D", "");
    if (digits.length() == 11) return isValidCpf(digits);
    if (digits.length() == 14) return isValidCnpj(digits);
    return false;
  }

  private static boolean isValidCpf(String cpf) {
    if (cpf.matches("(\\d)\\1{10}")) return false;
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += (cpf.charAt(i) - '0') * (10 - i);
    int dv = (sum * 10) % 11;
    if (dv == 10) dv = 0;
    if (dv != (cpf.charAt(9) - '0')) return false;
    sum = 0;
    for (int i = 0; i < 10; i++) sum += (cpf.charAt(i) - '0') * (11 - i);
    dv = (sum * 10) % 11;
    if (dv == 10) dv = 0;
    return dv == (cpf.charAt(10) - '0');
  }

  private static boolean isValidCnpj(String cnpj) {
    if (cnpj.matches("(\\d)\\1{13}")) return false;
    int[] w1 = {5,4,3,2,9,8,7,6,5,4,3,2};
    int[] w2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
    int sum = 0;
    for (int i = 0; i < 12; i++) sum += (cnpj.charAt(i) - '0') * w1[i];
    int dv = sum % 11;
    dv = dv < 2 ? 0 : 11 - dv;
    if (dv != (cnpj.charAt(12) - '0')) return false;
    sum = 0;
    for (int i = 0; i < 13; i++) sum += (cnpj.charAt(i) - '0') * w2[i];
    dv = sum % 11;
    dv = dv < 2 ? 0 : 11 - dv;
    return dv == (cnpj.charAt(13) - '0');
  }
}
