import React, { useEffect, useState } from 'react';
import './App.css';

function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchReservations = async () => {
      try {
        const response = await fetch('http://localhost:8080/reservations/all', {
          method: 'GET',
          credentials: 'include', 
        });

        if (!response.ok) {
          const errorBody = await response.text();
          throw new Error(`HTTP error! status: ${response.status}, body: ${errorBody}`);
        }

        const data = await response.json();
        setReservations(data);
      } catch (error) {
        console.error(error);
        setError(error);
      } finally {
        setLoading(false);
      }
    };

    fetchReservations();
  }, []); 

  if (loading) {
    return <div>Loading reservations...</div>;
  }

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  if (reservations.length === 0) {
    return <div>No reservations found.</div>;
  }

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return '-';
    try {
      const date = new Date(dateTimeString);
      if (isNaN(date.getTime())) {
         return '-';
      }
      return date.toLocaleString();
    } catch (e) {
      return '-';
    }
  };

  return (
    <div className="Reservations">
      <h2>Reservations</h2>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Start Time</th>
            <th>End Time</th>
            <th>Reservation Time</th>
          </tr>
        </thead>
        <tbody>
          {reservations.map((reservation) => (
            <tr key={reservation.id}>
              <td>{reservation.id}</td>
              <td>{reservation.username}</td>
              <td>{formatDateTime(reservation.startTime)}</td>
              <td>{formatDateTime(reservation.endTime)}</td>
              <td>{formatDateTime(reservation.reservationTime)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Reservations;