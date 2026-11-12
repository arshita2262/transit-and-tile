import { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom';
import { getHotelDetails, checkAvailability } from '../services/api';
import { getTodayString, getDefaultCheckOutString, calculateNights, formatCurrency } from '../utils/dateUtils';

export default function HotelDetails() {
  const { hotelId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const today = getTodayString();
  const initialCheckIn = searchParams.get('checkIn') || today;
  const initialCheckOut = searchParams.get('checkOut') || getDefaultCheckOutString(initialCheckIn);

  const [hotel, setHotel] = useState(null);
  const [activeImageIdx, setActiveImageIdx] = useState(0);
  const [checkIn, setCheckIn] = useState(initialCheckIn);
  const [checkOut, setCheckOut] = useState(initialCheckOut);
  const [availability, setAvailability] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checkingAvailability, setCheckingAvailability] = useState(false);
  const [error, setError] = useState('');
  const [apiError, setApiError] = useState('');

  // 1. Fetch hotel details
  useEffect(() => {
    let isMounted = true;
    async function fetchHotel() {
      try {
        setLoading(true);
        setError('');
        const data = await getHotelDetails(hotelId);
        if (isMounted) setHotel(data);
      } catch (err) {
        if (isMounted) setError(err.message || 'Hotel not found.');
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    fetchHotel();
    return () => { isMounted = false; };
  }, [hotelId]);

  // Derived validation error
  const dateError = !checkIn || !checkOut
    ? 'Dates required.'
    : checkIn < today
    ? 'Check-in date cannot be in the past.'
    : checkOut <= checkIn
    ? 'Check-out date must be strictly after check-in date.'
    : apiError;

  // 2. Check availability when valid dates change
  useEffect(() => {
    if (!hotelId || !checkIn || !checkOut || checkIn < today || checkOut <= checkIn) {
      return;
    }

    let isMounted = true;

    async function verifyAvailability() {
      try {
        setCheckingAvailability(true);
        setApiError('');
        const res = await checkAvailability(hotelId, checkIn, checkOut);
        if (isMounted) {
          setAvailability(res);
          // Keep URL params synchronized
          const params = new URLSearchParams(searchParams);
          params.set('checkIn', checkIn);
          params.set('checkOut', checkOut);
          setSearchParams(params, { replace: true });
        }
      } catch (err) {
        if (isMounted) {
          setApiError(err.message || 'Error checking availability.');
        }
      } finally {
        if (isMounted) setCheckingAvailability(false);
      }
    }

    verifyAvailability();
    return () => { isMounted = false; };
  }, [hotelId, checkIn, checkOut, today, searchParams, setSearchParams]);

  const nights = calculateNights(checkIn, checkOut);
  const pricePerNight = hotel?.roomType?.pricePerNight || 0;
  const estimatedTotal = availability?.estimatedTotalPrice || (pricePerNight * nights);

  const handleProceedToBooking = () => {
    if (!hotel || !hotel.roomType || !availability || !availability.available) return;

    navigate('/booking', {
      state: {
        hotelId: hotel.id,
        hotelName: hotel.name,
        hotelLocation: hotel.location,
        roomTypeId: hotel.roomType.id,
        roomName: hotel.roomType.roomName,
        pricePerNight: hotel.roomType.pricePerNight,
        checkIn,
        checkOut,
        nights,
        estimatedTotal,
      },
    });
  };

  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading hotel details...</span>
        </div>
        <p className="text-muted mt-2">Loading hotel sanctuary details...</p>
      </div>
    );
  }

  if (error || !hotel) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-warning py-4 mx-auto" style={{ maxWidth: '520px' }}>
          <h4>Hotel Not Found</h4>
          <p className="mb-3">{error || 'The requested hotel could not be found.'}</p>
          <Link to="/" className="btn btn-tt-primary btn-sm">
            ← Return to Home
          </Link>
        </div>
      </div>
    );
  }

  const images = hotel.imageUrls && hotel.imageUrls.length > 0
    ? hotel.imageUrls
    : ['https://images.unsplash.com/photo-1566073771259-6a8506099945'];

  return (
    <div className="hotel-details-page py-4">
      <div className="container">
        {/* Breadcrumb Navigation */}
        <nav aria-label="breadcrumb" className="mb-3">
          <ol className="breadcrumb small">
            <li className="breadcrumb-item"><Link to="/">Home</Link></li>
            <li className="breadcrumb-item"><Link to={`/search?location=${encodeURIComponent(hotel.location.split(',')[0])}`}>Search</Link></li>
            <li className="breadcrumb-item active" aria-current="page">{hotel.name}</li>
          </ol>
        </nav>

        {/* Title Header */}
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
          <div>
            <h1 className="h2 fw-bold mb-1">{hotel.name}</h1>
            <p className="text-muted mb-0">📍 {hotel.location}</p>
          </div>
          <div className="d-flex align-items-center gap-2">
            <div className="tt-rating-pill fs-6 px-3 py-2">
              ★ {Number(hotel.rating || 4.5).toFixed(1)} / 5.0
            </div>
            <span className="text-muted small">Guest Rating</span>
          </div>
        </div>

        {/* Image Gallery / Carousel */}
        <div className="row g-2 mb-4">
          <div className="col-lg-8">
            <div className="position-relative rounded overflow-hidden shadow-sm" style={{ height: '420px', background: '#000' }}>
              <img
                src={images[activeImageIdx]}
                alt={`${hotel.name} view ${activeImageIdx + 1}`}
                className="w-100 h-100 object-fit-cover"
              />
              <div className="position-absolute bottom-0 start-0 m-3 px-3 py-1 bg-dark bg-opacity-75 text-white rounded-pill small">
                Photo {activeImageIdx + 1} of {images.length}
              </div>
            </div>
          </div>
          <div className="col-lg-4 d-flex flex-lg-column gap-2 overflow-auto" style={{ maxHeight: '420px' }}>
            {images.map((img, idx) => (
              <button
                key={idx}
                type="button"
                className={`p-0 border-0 rounded overflow-hidden flex-shrink-0 ${activeImageIdx === idx ? 'border-primary shadow' : 'opacity-75'}`}
                style={{
                  height: '135px',
                  width: '100%',
                  cursor: 'pointer',
                  border: activeImageIdx === idx ? '3px solid #0284c7' : '1px solid #e2e8f0',
                }}
                onClick={() => setActiveImageIdx(idx)}
              >
                <img
                  src={img}
                  alt={`Thumbnail ${idx + 1}`}
                  className="w-100 h-100 object-fit-cover"
                />
              </button>
            ))}
          </div>
        </div>

        {/* Main Content Layout: Left Details & Right Booking Card */}
        <div className="row g-4">
          <div className="col-lg-8">
            {/* Description */}
            <div className="card p-4 border bg-white rounded shadow-sm mb-4">
              <h4 className="fw-bold h5 mb-3">About this sanctuary</h4>
              <p className="text-muted leading-relaxed mb-0">{hotel.description}</p>
            </div>

            {/* Room Category Information */}
            {hotel.roomType && (
              <div className="card p-4 border bg-white rounded shadow-sm mb-4">
                <div className="d-flex justify-content-between align-items-start mb-3">
                  <div>
                    <span className="badge bg-primary-subtle text-primary border border-primary-subtle mb-1">
                      Included Room Category
                    </span>
                    <h4 className="fw-bold h5 mb-1">{hotel.roomType.roomName}</h4>
                    <p className="text-muted small mb-0">Max Capacity: {hotel.roomType.capacity} Guests &bull; Total Rooms: {hotel.roomType.totalRooms}</p>
                  </div>
                  <div className="text-end">
                    <span className="h4 fw-bold text-dark">{formatCurrency(hotel.roomType.pricePerNight)}</span>
                    <span className="text-muted small d-block">/ night</span>
                  </div>
                </div>
                <p className="text-muted small mb-0">{hotel.roomType.description}</p>
              </div>
            )}

            {/* Amenities List */}
            {hotel.amenities && hotel.amenities.length > 0 && (
              <div className="card p-4 border bg-white rounded shadow-sm mb-4">
                <h4 className="fw-bold h5 mb-3">Sanctuary Amenities</h4>
                <div className="row g-3">
                  {hotel.amenities.map((amenity, idx) => (
                    <div key={idx} className="col-md-6 d-flex align-items-center gap-2">
                      <span className="text-primary fw-bold">✓</span>
                      <span className="text-dark small">{amenity}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Hotel Policies */}
            <div className="card p-4 border bg-white rounded shadow-sm mb-4">
              <h4 className="fw-bold h5 mb-3">Stay Policies</h4>
              <div className="row g-3 mb-3">
                <div className="col-6">
                  <span className="text-muted small d-block">Check-in Time</span>
                  <span className="fw-semibold text-dark">{hotel.checkInTime || '14:00:00'}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Check-out Time</span>
                  <span className="fw-semibold text-dark">{hotel.checkOutTime || '11:00:00'}</span>
                </div>
              </div>
              <div>
                <span className="text-muted small d-block mb-1">Cancellation Policy</span>
                <div className="p-3 bg-light rounded text-dark small border">
                  {hotel.cancellationPolicy}
                </div>
              </div>
            </div>
          </div>

          {/* Right Sticky Booking Calculator Card */}
          <div className="col-lg-4">
            <div className="card border p-4 bg-white rounded shadow-sm sticky-top" style={{ top: '80px' }}>
              <div className="d-flex justify-content-between align-items-baseline mb-3 pb-2 border-bottom">
                <div>
                  <span className="h3 fw-bold text-dark">{formatCurrency(pricePerNight)}</span>
                  <span className="text-muted small"> / night</span>
                </div>
                {availability && (
                  <div>
                    {availability.available ? (
                      <span className="tt-badge-available">
                        ● {availability.availableRooms} Available
                      </span>
                    ) : (
                      <span className="tt-badge-soldout">
                        ● Sold Out
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* Date Selector Form */}
              <div className="mb-3">
                <label htmlFor="detailCheckIn" className="form-label small fw-bold text-muted">Check-in</label>
                <input
                  id="detailCheckIn"
                  type="date"
                  className="form-control tt-search-input"
                  min={today}
                  value={checkIn}
                  onChange={(e) => setCheckIn(e.target.value)}
                />
              </div>

              <div className="mb-3">
                <label htmlFor="detailCheckOut" className="form-label small fw-bold text-muted">Check-out</label>
                <input
                  id="detailCheckOut"
                  type="date"
                  className="form-control tt-search-input"
                  min={checkIn}
                  value={checkOut}
                  onChange={(e) => setCheckOut(e.target.value)}
                />
              </div>

              {dateError && (
                <div className="alert alert-danger py-2 px-3 mb-3 small" role="alert">
                  {dateError}
                </div>
              )}

              {/* Live Price Breakdown */}
              <div className="bg-light p-3 rounded mb-3 border">
                <div className="d-flex justify-content-between text-muted small mb-2">
                  <span>Stay Duration</span>
                  <span className="fw-semibold text-dark">
                    {nights} {nights === 1 ? 'Night' : 'Nights'}
                  </span>
                </div>
                <div className="d-flex justify-content-between text-muted small mb-2">
                  <span>{formatCurrency(pricePerNight)} &times; {nights} {nights === 1 ? 'night' : 'nights'}</span>
                  <span>{formatCurrency(pricePerNight * nights)}</span>
                </div>
                <div className="d-flex justify-content-between text-muted small mb-2">
                  <span>Taxes & Service Fees</span>
                  <span className="text-success fw-semibold">Included</span>
                </div>
                <div className="d-flex justify-content-between fw-bold pt-2 border-top text-dark">
                  <span>Estimated Total</span>
                  <span>{formatCurrency(estimatedTotal)}</span>
                </div>
                <small className="text-muted d-block mt-2" style={{ fontSize: '0.72rem' }}>
                  * Provisional estimate. Final total is authoritatively verified and locked by the backend during reservation.
                </small>
              </div>

              {/* Action Button */}
              {checkingAvailability ? (
                <button type="button" className="btn btn-secondary w-100 py-2" disabled>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Checking live availability...
                </button>
              ) : availability && availability.available ? (
                <button
                  type="button"
                  className="btn btn-tt-primary w-100 py-2 fs-6"
                  onClick={handleProceedToBooking}
                >
                  Reserve Stay →
                </button>
              ) : (
                <button type="button" className="btn btn-outline-danger w-100 py-2" disabled>
                  Sold Out for Selected Dates
                </button>
              )}

              <div className="text-center mt-3">
                <small className="text-muted">
                  🔒 Instant confirmation &bull; No upfront card required
                </small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
