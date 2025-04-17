import React, { useState, useEffect } from 'react';
import "./App.css";

function Main() {
  const [src, setSrc] = useState('');
  const [dest, setDest] = useState('');
  const [time, setTime] = useState('');
  const [results, setResults] = useState([]);
  const [searches, setSearches] = useState(0);

  function formatDate(date) {
    const pad = (n) => n.toString().padStart(2, '0');
  
    const day = pad(date.getDate());
    const month = pad(date.getMonth() + 1); // Months are 0-based
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
  
    return `${day}-${month} ${hours}:${minutes}`;
  }
  

  const handleSearch = () => {
    setResults([]);
    setSearches((prevSearches) => prevSearches + 1);
    const eventSource = new EventSource(`http://localhost:8080/query/offers/src/${encodeURIComponent(src)}/dest/${encodeURIComponent(dest)}/time/${encodeURIComponent(time)}`, {
      withCredentials: true
    });

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setResults((prevResults) => [...prevResults, ...data]);
      console.log('New offers received:', data);
    };

    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      setSearches((prevSearches) => prevSearches - 1);
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
      {(searches > 0) && <p>Loading...</p>}

      {results.length > 0 && (
        <div>
          <h3>Results:</h3>
          <table>
            <thead>
              <tr>
                <th>Source</th>
                <th>Destination</th>
                <th>Start Time</th>
                <th>End Time</th>
                <th>Cost</th>
                <th>Type</th>
                <th>Vehicles</th>
              </tr>
            </thead>
            <tbody>
              {results.map((offer, offerIndex) => (
                <tr key={offerIndex}>
                  <td>{offer.src}</td>
                  <td>{offer.dest}</td>
                  <td>{formatDate(new Date(offer.startTime))}</td>
                  <td>{formatDate(new Date(offer.endTime))}</td>
                  <td>{offer.cost}</td>
                  <td>{offer.type}</td>
                  <td>
                    <table>
                      <thead>
                        <tr>
                          <th>Vehicle ID</th>
                          <th>Start</th>
                          <th>End</th>
                        </tr>
                      </thead>
                      <tbody>
                        {offer.vehicles.map((vehicle, vIndex) => (
                          <tr key={vIndex}>
                            <td>{vehicle.id}</td>
                            <td>{formatDate(new Date(vehicle.start))}</td>
                            <td>{formatDate(new Date(vehicle.end))}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

        </div>
      )}
    </div>
  );
}

export default Main;