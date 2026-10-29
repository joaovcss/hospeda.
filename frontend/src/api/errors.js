const STATUS_MESSAGES = {
  0: 'Não foi possível conectar ao servidor. Verifique se o backend está rodando.',
  401: 'Sessão expirada ou credenciais inválidas.',
  403: 'O servidor recusou a requisição (sem permissão ou origem não autorizada).',
  404: 'Recurso não encontrado no servidor.',
};

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || STATUS_MESSAGES[status]
      || (status >= 500 ? 'Não foi possível conectar ao servidor. Verifique se o backend está rodando.' : `Erro inesperado (código ${status}). Tente novamente.`));
    this.status = status;
    this.fieldErrors = body?.fieldErrors || [];
  }
}
