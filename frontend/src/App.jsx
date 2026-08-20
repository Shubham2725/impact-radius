import { useState, useEffect } from 'react';
import './App.css';

const API_BASE = 'https://impact-radius.onrender.com/api';

function App() {
  const [servers, setServers] = useState([]);
  const [selectedServer, setSelectedServer] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`${API_BASE}/servers`)
      .then((res) => res.json())
      .then((data) => setServers(data))
      .catch(() => setError('Could not load server list. Is the backend running?'));
  }, []);

  const checkImpact = async () => {
    if (!selectedServer) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await fetch(`${API_BASE}/blast-radius/${selectedServer}`);
      if (!res.ok) throw new Error('Server error');
      const data = await res.json();
      setResult(data);
    } catch {
      setError('Could not reach the backend. Please make sure it is running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <h1>Impact Radius</h1>
      <p className="subtitle">See what breaks when a server goes down</p>

      <div className="controls">
        <select value={selectedServer} onChange={(e) => setSelectedServer(e.target.value)}>
          <option value="">Select a server…</option>
          {servers.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <button onClick={checkImpact} disabled={!selectedServer || loading}>
          {loading ? 'Checking…' : 'Check Impact'}
        </button>
      </div>

      {error && <div className="error-box">{error}</div>}

      {result && !result.message && (
        <div className="results">
          <h2>Impact of {result.server} going down</h2>

          <Section title="Directly Affected Services" items={result.directlyAffectedServices} />
          <Section title="Upstream Affected Services" items={result.upstreamAffectedServices} />
          <Section title="Affected Applications" items={result.affectedApplications} />
          <Section title="Teams to Notify" items={result.teamsToNotify} />
        </div>
      )}

      {result && result.message && (
        <div className="empty-box">{result.message}</div>
      )}
    </div>
  );
}

function Section({ title, items }) {
  const filtered = (items || []).filter(Boolean);
  return (
    <div className="section">
      <h3>{title}</h3>
      {filtered.length === 0 ? (
        <p className="empty-text">None</p>
      ) : (
        <ul>
          {filtered.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default App;
