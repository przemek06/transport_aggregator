import React, { useEffect, useState } from 'react';
import './App.css';

function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
        console.error("Error fetching initial reservations:", error);
        setError(error);
      } finally {
        setLoading(false);
      }
    };

    fetchReservations();

    const eventSource = new EventSource('http://localhost:8080/reservations/updates', {
      withCredentials: true
    });

    eventSource.onmessage = (event) => {
      const update = JSON.parse(event.data);
      console.log('Reservation update received:', update);

      setReservations((prevReservations) => {
        let updatedReservations = [...prevReservations];

        switch (update.operation) {
          case 'CREATE':
            if (update.inserted) {
              updatedReservations.push(update.inserted);
            }
            break;
          case 'UPDATE':
            if (update.updated) {
              updatedReservations = updatedReservations.map((res) =>
                res.id === update.updated.id ? update.updated : res
              );
            }
            break;
          case 'DELETE':
            if (update.deleted) {
              updatedReservations = updatedReservations.filter((res) =>
                res.id !== update.deleted
              );
            }
            break;
          default:
            console.warn('Unknown reservation operation:', update.operation);
        }
        return updatedReservations;
      });
    };

    eventSource.onerror = (error) => {
      console.error('SSE error for reservations:', error);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
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

  return (
    <div className="Reservations">
      <h2>Reservations</h2>
      <table>
        <thead>
          <tr>
            <th>Offer ID</th>
            <th>Username</th>
            <th>Start Time</th>
            <th>End Time</th>
            <th>Start station</th>
            <th>End station</th>
            <th>Cost</th>
            <th>Reservation Time</th>
          </tr>
        </thead>
        <tbody>
          {reservations.map((reservation) => (
            <tr key={reservation.id}>
              <td>{reservation.offerId}</td>
              <td>{reservation.username}</td>
              <td>{formatDateTime(reservation.startTime)}</td>
              <td>{formatDateTime(reservation.endTime)}</td>
              <td>{reservation.src}</td>
              <td>{reservation.dest}</td>
              <td>{reservation.cost}</td>
              <td>{formatDateTime(reservation.reservationTime)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Reservations;