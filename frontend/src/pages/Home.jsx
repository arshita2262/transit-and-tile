import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import SearchBar from '../components/SearchBar';
import HotelCard from '../components/HotelCard';
import { getHotels } from '../services/api';
import { getTodayString, getDefaultCheckOutString } from '../utils/dateUtils';

export default function Home() {
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const today = getTodayString();
  const defaultCheckOut = getDefaultCheckOutString(today);

  useEffect(() => {
    let isMounted = true;
    async function loadHotels() {
      try {
        setLoading(true);
        const data = await getHotels();
        if (isMounted) {
          setHotels(data);
          setError('');
        }
      } catch (err) {
        if (isMounted) {
          setError(err.message || 'Unable to connect to backend service.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    loadHotels();
    return () => { isMounted = false; };
  }, []);

  const popularDestinations = [
    { name: 'Goa', tag: 'Sun & Sea', icon: '🌴', location: 'Goa' },
    { name: 'Jaipur', tag: 'Heritage Royalty', icon: '🏰', location: 'Jaipur' },
    { name: 'Manali', tag: 'Pine Hills', icon: '🏔️', location: 'Manali' },
    { name: 'Munnar', tag: 'Tea Plantations', icon: '🍵', location: 'Munnar' },
    { name: 'Udaipur', tag: 'Lake Palaces', icon: '⛵', location: 'Udaipur' },
  ];

  return (
    <div className="home-page">
      {/* Search Header Banner (Clean, Sky-Blue tinted, No giant hero image) */}
      <section className="py-5" style={{ background: 'linear-gradient(180deg, #e0f2fe 0%, #f0f9ff 60%, #f8fafc 100%)' }}>
        <div className="container text-center">
          <div className="d-inline-flex align-items-center gap-2 bg-white px-3 py-1 rounded-pill shadow-sm mb-3">
            <span className="badge bg-primary text-white">New</span>
            <small className="fw-semibold text-secondary">
              Destination Postcard Confirmation included with every booking
            </small>
          </div>
          <h1 className="display-5 fw-extrabold text-dark mb-2" style={{ letterSpacing: '-0.5px' }}>
            Find Your Next Sanctuary
          </h1>
          <p className="lead text-muted mx-auto mb-4" style={{ maxWidth: '620px' }}>
            Thoughtfully curated heritage havelis, seaside retreats, and mountain lodges across India with authoritative date-based pricing.
          </p>

          {/* Centred Search Bar */}
          <div className="mx-auto" style={{ maxWidth: '960px' }}>
            <SearchBar initialCheckIn={today} initialCheckOut={defaultCheckOut} />
          </div>

          {/* Popular Destination Quick-Pills */}
          <div className="d-flex flex-wrap justify-content-center align-items-center gap-2 mt-3">
            <span className="small text-muted fw-bold me-1">Popular Escapes:</span>
            {popularDestinations.map((dest) => (
              <Link
                key={dest.name}
                to={`/search?location=${encodeURIComponent(dest.location)}&checkIn=${today}&checkOut=${defaultCheckOut}`}
                className="btn btn-sm btn-light border bg-white shadow-sm rounded-pill px-3 py-1 text-dark"
                style={{ fontSize: '0.85rem' }}
              >
                <span className="me-1">{dest.icon}</span>
                <span className="fw-semibold">{dest.name}</span>
                <span className="text-muted ms-1 small">({dest.tag})</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Explore Stays Section */}
      <section className="py-5 container">
        <div className="d-flex justify-content-between align-items-end mb-4">
          <div>
            <h2 className="h3 fw-bold mb-1">Explore Stays</h2>
            <p className="text-muted mb-0">
              Handpicked boutique hotels with verified room inventory
            </p>
          </div>
          <span className="text-muted small">
            {hotels.length} {hotels.length === 1 ? 'Stay' : 'Stays'} Available
          </span>
        </div>

        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading stays...</span>
            </div>
            <p className="text-muted mt-2 small">Connecting to backend and loading stays...</p>
          </div>
        ) : error ? (
          <div className="alert alert-warning py-4 text-center">
            <h5 className="alert-heading">Notice</h5>
            <p className="mb-0">{error}</p>
          </div>
        ) : hotels.length === 0 ? (
          <div className="text-center py-5 bg-white rounded border">
            <p className="text-muted mb-0">No hotels found in the catalog.</p>
          </div>
        ) : (
          <div className="row g-4">
            {hotels.map((hotel) => (
              <div key={hotel.id} className="col-12 col-md-6 col-lg-4">
                <HotelCard
                  hotel={hotel}
                  checkIn={today}
                  checkOut={defaultCheckOut}
                />
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Transit & Tile Difference */}
      <section className="py-5 bg-white border-top border-bottom">
        <div className="container">
          <div className="text-center mb-5">
            <h2 className="h4 fw-bold">The Transit & Tile Promise</h2>
            <p className="text-muted small">Built on transactional integrity and thoughtful travel design</p>
          </div>
          <div className="row g-4 text-center">
            <div className="col-md-4">
              <div className="p-3">
                <div className="fs-1 mb-2">🔒</div>
                <h5 className="fw-bold h6">Authoritative Concurrency Protection</h5>
                <p className="text-muted small mb-0">
                  Real-time pessimistic room-level locking prevents overbooking and preserves exact inventory.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="p-3">
                <div className="fs-1 mb-2">🏷️</div>
                <h5 className="fw-bold h6">Transparent Historical Pricing</h5>
                <p className="text-muted small mb-0">
                  Every booking locks in the exact night price calculated by the server at checkout. No surprise adjustments.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="p-3">
                <div className="fs-1 mb-2">📮</div>
                <h5 className="fw-bold h6">Vintage Postcard Confirmation</h5>
                <p className="text-muted small mb-0">
                  Receive a bespoke destination keepsake with your confirmed booking reference and curated local tips.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
