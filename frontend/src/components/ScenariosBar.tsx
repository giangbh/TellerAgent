import React from 'react';
import { Sparkles, ArrowRight, AlertCircle, FileText, Banknote, ArrowDownToLine, Coins, MapPin, Layers } from 'lucide-react';
import { ScenarioItem } from '../types/teller';

interface ScenariosBarProps {
  scenarios: ScenarioItem[];
  onSelectScenario: (prompt: string) => void;
  disabled: boolean;
}

export const ScenariosBar: React.FC<ScenariosBarProps> = ({ scenarios, onSelectScenario, disabled }) => {
  const getIcon = (id: string) => {
    switch (id) {
      case 'dynamic-fx': return <Coins size={14} color="#f59e0b" />;
      case 'dynamic-branch': return <MapPin size={14} color="#10b981" />;
      case 'dynamic-summary': return <Layers size={14} color="#8b5cf6" />;
      case 'cash-deposit': return <Banknote size={14} color="#34d399" />;
      case 'cash-withdrawal': return <ArrowDownToLine size={14} color="#38bdf8" />;
      case 'transfer-complete': return <Sparkles size={14} color="#818cf8" />;
      case 'transfer-missing': return <ArrowRight size={14} color="#fbbf24" />;
      case 'risk-review': return <AlertCircle size={14} color="#f87171" />;
      case 'policy': return <FileText size={14} color="#c084fc" />;
      default: return <Sparkles size={14} />;
    }
  };

  return (
    <div style={{ marginBottom: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
        <span style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-dim)' }}>
          Kịch bản & Tác vụ Động (Dynamic Autonomous):
        </span>
      </div>
      <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px' }}>
        {scenarios.map((sc) => (
          <button
            key={sc.id}
            onClick={() => onSelectScenario(sc.prompt)}
            disabled={disabled}
            className="btn btn-secondary"
            style={{
              padding: '7px 12px',
              fontSize: '0.8rem',
              whiteSpace: 'nowrap',
              borderRadius: '8px',
              background: 'rgba(255, 255, 255, 0.03)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              transition: 'all 0.15s ease',
            }}
          >
            {getIcon(sc.id)}
            <span>{sc.name}</span>
          </button>
        ))}
      </div>
    </div>
  );
};
