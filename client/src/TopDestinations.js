import React, { useEffect, useState } from "react";

export default function TopDestinations() {
  const [topDestinations, setTopDestinations] = useState([]);

  useEffect(() => {
    const eventSource = new EventSource("http://localhost:8080/stream/top-destinations");

    eventSource.addEventListener("top-destinations", (event) => {
      try {
        const top5 = JSON.parse(event.data); // array of destination strings
        setTopDestinations(top5);
      } catch (e) {
        console.error("Error parsing top destinations:", e);
      }
    });

    eventSource.onerror = (err) => {
      console.error("SSE connection error (top destinations):", err);
      eventSource.close();
    };

    return () => eventSource.close();
  }, []);

  return (
    <div style={{ marginTop: 32, marginBottom: 32 }}>
      <h2>Top 5 Popular Destinations (Live)</h2>
      <ol>
        {topDestinations.length === 0 && <li>No data yet</li>}
        {topDestinations.map((dest, idx) => (
          <li key={idx}>{dest}</li>
        ))}
      </ol>
    </div>
  );
}
