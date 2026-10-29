import { useState } from 'react';
import { History } from 'lucide-react';
import { accessApi, areasApi } from '../api/client';
import { Avatar, Badge, Card, EmptyState, ErrorState, PageHeader, Pagination, Spinner } from '../components/ui';
import { formatDateTime } from '../utils/format';
import { useApi } from '../utils/useApi';

export default function AccessLogs() {
  const [filters, setFilters] = useState({ result: '', areaId: '' });
  const [page, setPage] = useState(0);
  const { data: areas } = useApi(() => areasApi.list().then((p) => p.content), []);
  const { data, error, loading, reload } = useApi(() => accessApi.logs({ ...filters, page, size: 15 }), [filters.result, filters.areaId, page]);

  const set = (key) => (e) => {
    setPage(0);
    setFilters({ ...filters, [key]: e.target.value });
  };

  return (
    <>
      <PageHeader title="Histórico de acessos" subtitle="Todas as tentativas de entrada nas áreas do hotel, liberadas ou negadas." />
      <Card padded={false} className="card-table">
        <div className="toolbar">
          <select className="select input-sm" value={filters.result} onChange={set('result')}>
            <option value="">Todos os resultados</option>
            <option value="GRANTED">Liberados</option>
            <option value="DENIED">Negados</option>
          </select>
          <select className="select input-sm" value={filters.areaId} onChange={set('areaId')}>
            <option value="">Todas as áreas</option>
            {areas?.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>
        {loading ? <Spinner /> : error ? <ErrorState error={error} onRetry={reload} /> : data.content.length === 0 ? (
          <EmptyState icon={History} title="Nenhum registro encontrado" />
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Hóspede</th><th>Área</th><th>Data e hora</th><th>Resultado</th><th>Registrado por</th></tr></thead>
                <tbody>
                  {data.content.map((log) => (
                    <tr key={log.id}>
                      <td><div className="cell-main"><Avatar name={log.guestName} size="sm" /><strong>{log.guestName}</strong></div></td>
                      <td>{log.areaName}</td>
                      <td>{formatDateTime(log.attemptedAt)}</td>
                      <td>
                        <div className="cell-stack" style={{ alignItems: 'flex-start', gap: 4 }}>
                          <Badge tone={log.result === 'GRANTED' ? 'green' : 'red'}>{log.result === 'GRANTED' ? 'Liberado' : 'Negado'}</Badge>
                          {log.denialMessage && <span>{log.denialMessage}</span>}
                        </div>
                      </td>
                      <td className="muted">{log.registeredByName || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data} onChange={setPage} />
          </>
        )}
      </Card>
    </>
  );
}
