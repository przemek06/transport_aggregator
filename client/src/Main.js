import React, { useState, useEffect } from 'react';

function Main() {
  const [src, setSrc] = useState('');
  const [dest, setDest] = useState('');
  const [time, setTime] = useState('');
  const [results, setResults] = useState([]);

  const handleSearch = () => {
    setResults([]);
    const eventSource = new EventSource(`http://localhost:8080/query/offers/src/${encodeURIComponent(src)}/dest/${encodeURIComponent(dest)}/time/${encodeURIComponent(time)}`, {
      withCredentials: true 
    });

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setResults((prevResults) => [...prevResults, data]);
    };

    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      eventSource.close();
    };

    return () => {
        eventSource.close();
    };
  };

  return (
    <div>
      <h2>Search Offers</h2>
      <div>
        <label>Source:</label>
        <input type="text" value={src} onChange={(e) => setSrc(e.target.value)} />
      </div>
      <div>
        <label>Destination:</label>
        <input type="text" value={dest} onChange={(e) => setDest(e.target.value)} />
      </div>
      <div>
        <label>Time:</label>
        <input type="datetime-local" value={time} onChange={(e) => setTime(e.target.value)} />
      </div>
      <button onClick={handleSearch}>Search</button>

      {results.length > 0 && (
        <div>
          <h3>Results:</h3>
          <table>
            <thead>
              <tr>
                <th>Source</th>
                <th>Destination</th>
                <th>Time</th>
                <th>Cost</th>
              </tr>
            </thead>
            <tbody>
              {results.map((result, outerIndex) => (
                result.map((row, index) => (
                <tr key={outerIndex*result.length + index}>
                  <td>{row.src}</td>
                  <td>{row.dest}</td>
                  <td>{row.time}</td>
                  <td>{row.cost}</td>
                  </tr>
                ))
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default Main;