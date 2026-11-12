import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTodayString, getDefaultCheckOutString } from '../utils/dateUtils';

export default function SearchBar({
  initialLocation = '',
  initialCheckIn = '',
  initialCheckOut = '',
  compact = false,
  onSearch = null,
}) {
  const navigate = useNavigate();
  const today = getTodayString();

  const [location, setLocation] = useState(initialLocation);
  const [checkIn, setCheckIn] = useState(initialCheckIn || today);
  const [checkOut, setCheckOut] = useState(initialCheckOut || getDefaultCheckOutString(initialCheckIn || today));
  const [error, setError] = useState('');

  // Adjust checkOut automatically if checkIn moves past checkOut
  const handleCheckInChange = (e) => {
    const newCheckIn = e.target.value;
    setCheckIn(newCheckIn);
    setError('');
    if (checkOut <= newCheckIn) {
      setCheckOut(getDefaultCheckOutString(newCheckIn));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError('');

    if (checkIn < today) {
      setError('Check-in date cannot be in the past.');
      return;
    }
    if (checkOut <= checkIn) {
      setError('Check-out date must be strictly after check-in date.');
      return;
    }

    if (onSearch) {
      onSearch({ location: location.trim(), checkIn, checkOut });
    } else {
      const params = new URLSearchParams();
      if (location.trim()) params.append('location', location.trim());
      params.append('checkIn', checkIn);
      params.append('checkOut', checkOut);
      navigate(`/search?${params.toString()}`);
    }
  };

  return (
    <div className={`tt-search-container ${compact ? 'py-3' : 'py-4'}`}>
      <form onSubmit={handleSubmit}>
        <div className="row g-2 align-items-end">
          <div className={compact ? 'col-md-4' : 'col-md-4'}>
            <label className="form-label small fw-bold text-muted mb-1">
              Destination / Hotel
            </label>
            <div className="input-group">
              <span className="input-group-text bg-white border-end-0">📍</span>
              <input
                type="text"
                className="form-control tt-search-input border-start-0"
                placeholder="Where to? (e.g. Goa, Jaipur)"
                value={location}
                onChange={(e) => {
                  setLocation(e.target.value);
                  setError('');
                }}
              />
            </div>
          </div>

          <div className={compact ? 'col-md-3 col-6' : 'col-md-3 col-6'}>
            <label className="form-label small fw-bold text-muted mb-1">
              Check-in Date
            </label>
            <input
              type="date"
              className="form-control tt-search-input"
              min={today}
              value={checkIn}
              onChange={handleCheckInChange}
              required
            />
          </div>

          <div className={compact ? 'col-md-3 col-6' : 'col-md-3 col-6'}>
            <label className="form-label small fw-bold text-muted mb-1">
              Check-out Date
            </label>
            <input
              type="date"
              className="form-control tt-search-input"
              min={checkIn}
              value={checkOut}
              onChange={(e) => {
                setCheckOut(e.target.value);
                setError('');
              }}
              required
            />
          </div>

          <div className={compact ? 'col-md-2' : 'col-md-2'}>
            <button type="submit" className="btn btn-tt-primary w-100">
              {compact ? 'Search' : 'Find Stays'}
            </button>
          </div>
        </div>

        {error && (
          <div className="alert alert-danger py-2 px-3 mt-2 mb-0 small" role="alert">
            {error}
          </div>
        )}
      </form>
    </div>
  );
}
