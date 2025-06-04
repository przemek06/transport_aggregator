import React, { useState } from 'react';

const createEmptyInsertOffer = () => ({
  src: '',
  dest: '',
  startTime: '',
  endTime: '',
  cost: '',
  vehicleId: '',
  type: '',
  maxSeats: ''
});

const createEmptyUpdateOffer = () => ({
  id: '',
  price: '',
  delay: '',
  maxSeats: ''
});

function Import() {
  const [toInsert, setToInsert] = useState([createEmptyInsertOffer()]);
  const [toUpdate, setToUpdate] = useState([createEmptyUpdateOffer()]);
  const [toDelete, setToDelete] = useState([{ id: '' }]);

  const handleImport = async () => {
    try {
      const parsedToInsert = toInsert
        .filter(row => Object.values(row).some(value => value !== ''))
        .map(row => {
          const vehicleDto = row.vehicleId ? [{ id: parseInt(row.vehicleId, 10), start: row.startTime, end: row.endTime }] : [];

          return {
            src: row.src,
            dest: row.dest,
            startTime: row.startTime ? new Date(row.startTime).toISOString() : null,
            endTime: row.endTime ? new Date(row.endTime).toISOString() : null,
            cost: row.cost !== '' ? parseFloat(row.cost) : null,
            vehicles: vehicleDto,
            type: row.type,
            maxSeats: row.maxSeats !== '' ? parseInt(row.maxSeats, 10) : null,
          };
        });

      const parsedToUpdate = toUpdate
        .filter(row => Object.values(row).some(value => value !== ''))
        .map(row => {
          return {
            ...row,
            id: row.id !== '' ? parseInt(row.id, 10) : null,
            price: row.price !== '' ? parseInt(row.price, 10) : null,
            delay: row.delay !== '' ? parseInt(row.delay, 10) : null,
            maxSeats: row.maxSeats !== '' ? parseInt(row.maxSeats, 10) : null,
          };
        });

      const parsedToDelete = toDelete
        .filter(row => row.id !== '')
        .map(row => parseInt(row.id, 10));

      const importCommand = {
        toCreate: parsedToInsert,
        toUpdate: parsedToUpdate,
        toDelete: parsedToDelete,
      };

      console.log('Sending import command:', importCommand);

      const response = await fetch('http://localhost:8080/import', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(importCommand),
        credentials: 'include'
      });

      if (response.ok) {
        alert('Import successful!');
        setToInsert([createEmptyInsertOffer()]);
        setToUpdate([createEmptyUpdateOffer()]);
        setToDelete([{ id: '' }]);
      } else {
        const errorText = await response.text();
        alert(`Import failed: ${response.status} - ${errorText}`);
      }
    } catch (error) {
      console.error('Error during import:', error);
      alert('An error occurred during import. Check console for details.');
    }
  };

  const handleGenerate = async () => {
    const no = prompt('Enter the number of elements to generate:');
    if (!no || isNaN(no) || parseInt(no, 10) <= 0) {
      alert('Please enter a valid positive number.');
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/import/generate/${no}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include'
      });
      if (response.ok) {
        const generatedOffers = await response.json();
        const formattedOffers = generatedOffers.map(offer => ({
          ...offer,
          startTime: new Date(offer.startTime).toISOString().slice(0, 16),
          endTime: new Date(offer.endTime).toISOString().slice(0, 16),
          cost: offer.cost.toString(),
          maxSeats: offer.maxSeats.toString(),
          vehicleId: offer.vehicles && offer.vehicles.length > 0 ? offer.vehicles[0].id.toString() : '',
        }));
        setToInsert(formattedOffers.length > 0 ? formattedOffers : [createEmptyInsertOffer()]);
      } else {
        const errorText = await response.text();
        alert(`Generate failed: ${response.status} - ${errorText}`);
      }
    } catch (error) {
      console.error('Error during generation:', error);
      alert('An error occurred during generation. Check console for details.');
    }
  };

  const handleInsertChange = (index, event) => {
    const { name, value } = event.target;
    const list = [...toInsert];
    list[index][name] = value;
    setToInsert(list);
  };

  const addInsertRow = () => {
    setToInsert([...toInsert, createEmptyInsertOffer()]);
  };

  const removeInsertRow = (index) => {
    const list = [...toInsert];
    list.splice(index, 1);
    setToInsert(list);
  };

  const handleUpdateChange = (index, event) => {
    const { name, value } = event.target;
    const list = [...toUpdate];
    list[index][name] = value;
    setToUpdate(list);
  };

  const addUpdateRow = () => {
    setToUpdate([...toUpdate, createEmptyUpdateOffer()]);
  };

  const removeUpdateRow = (index) => {
    const list = [...toUpdate];
    list.splice(index, 1);
    setToUpdate(list);
  };

  const handleDeleteChange = (index, event) => {
    const { value } = event.target;
    const list = [...toDelete];
    list[index].id = value;
    setToDelete(list);
  };

  const addDeleteRow = () => {
    setToDelete([...toDelete, { id: '' }]);
  };

  const removeDeleteRow = (index) => {
    const list = [...toDelete];
    list.splice(index, 1);
    setToDelete(list);
  };

  return (
    <div style={styles.container}>
      <h1>Import Data</h1>

      <div style={styles.buttonContainer}>
        <button onClick={handleImport}>Import</button>
        <button onClick={handleGenerate}>Generate</button>
      </div>

      <div style={styles.sectionSeparator} />

      <section style={styles.section}>
        <h2>To Insert</h2>
        <table style={styles.table}>
          <thead>
            <tr>
              <th>Src</th>
              <th>Dest</th>
              <th>Start Time</th>
              <th>End Time</th>
              <th>Cost</th>
              <th>Vehicle ID</th>
              <th>Vehicle Type</th>
              <th>Max Seats</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {toInsert.map((offer, index) => (
              <tr key={index}>
                <td><input type="text" name="src" value={offer.src} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td><input type="text" name="dest" value={offer.dest} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td><input type="datetime-local" name="startTime" value={offer.startTime} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td><input type="datetime-local" name="endTime" value={offer.endTime} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td><input type="number" name="cost" value={offer.cost} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td><input type="number" name="vehicleId" value={offer.vehicleId} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td><input type="text" name="type" value={offer.type} onChange={(e) => handleInsertChange(index, e)} placeholder='TRAIN or BUS' style={styles.input} /></td>
                <td><input type="number" name="maxSeats" value={offer.maxSeats} onChange={(e) => handleInsertChange(index, e)} style={styles.input} /></td>
                <td>
                  <button onClick={() => removeInsertRow(index)} style={styles.button}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <button onClick={addInsertRow} style={styles.button}>Add Row</button>
      </section>

      <div style={styles.sectionSeparator} />

      <section style={styles.section}>
        <h2>To Update</h2>
        <table style={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Price</th>
              <th>Delay</th>
              <th>Max Seats</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {toUpdate.map((offer, index) => (
              <tr key={index}>
                <td><input type="number" name="id" value={offer.id} onChange={(e) => handleUpdateChange(index, e)} style={styles.input} /></td>
                <td><input type="number" name="price" value={offer.price} onChange={(e) => handleUpdateChange(index, e)} style={styles.input} /></td>
                <td><input type="number" name="delay" value={offer.delay} onChange={(e) => handleUpdateChange(index, e)} style={styles.input} /></td>
                <td><input type="number" name="maxSeats" value={offer.maxSeats} onChange={(e) => handleUpdateChange(index, e)} style={styles.input} /></td>
                <td>
                  <button onClick={() => removeUpdateRow(index)} style={styles.button}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <button onClick={addUpdateRow} style={styles.button}>Add Row</button>
      </section>

      <div style={styles.sectionSeparator} />

      <section style={styles.section}>
        <h2>To Delete</h2>
        <table style={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {toDelete.map((item, index) => (
              <tr key={index}>
                <td><input type="number" name="id" value={item.id} onChange={(e) => handleDeleteChange(index, e)} style={styles.input} /></td>
                <td>
                  <button onClick={() => removeDeleteRow(index)} style={styles.button}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <button onClick={addDeleteRow} style={styles.button}>Add Row</button>
      </section>
    </div>
  );
}

const styles = {
  container: {
    padding: '20px',
  },
  buttonContainer: {
    marginBottom: '20px',
  },
  button: {
    padding: '8px 15px',
    marginRight: '10px',
    cursor: 'pointer',
  },
  section: {
    marginBottom: '30px',
    border: '1px solid #ddd',
    padding: '15px',
    borderRadius: '5px',
  },
  sectionSeparator: {
    margin: '40px 0',
    borderBottom: '1px solid #eee',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    marginBottom: '10px',
  },
  input: {
    width: 'calc(100% - 10px)',
    padding: '5px',
    boxSizing: 'border-box',
  },
  th: {
    border: '1px solid #ddd',
    padding: '8px',
    textAlign: 'left',
    backgroundColor: '#f2f2f2',
  },
  td: {
    border: '1px solid #ddd',
    padding: '8px',
  },
};

export default Import;