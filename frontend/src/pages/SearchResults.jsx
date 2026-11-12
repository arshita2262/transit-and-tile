import { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import SearchBar from '../components/SearchBar';
import HotelCard from '../components/HotelCard';
import { getHotels, checkAvailability } from '../services/api';
import { getTodayString, getDefaultCheckOutString, calculateNights } from '../utils/dateUtils';

export default function SearchResults() {
  const [searchParams, setSearchParams] = useSearchParams();

  const locationParam = searchParams.get('location') || '';
  const today = getTodayString();
  const checkInParam = searchParams.get('checkIn') || today;
  const checkOutParam = searchParams.get('checkOut') || getDefaultCheckOutString(checkInParam);

  const [hotels, setHotels] = useState([]);
  const [availabilities, setAvailabilities] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters & Sorting state
  const [sortBy, setSortBy] = useState('price-asc'); // price-asc, price-desc, rating-desc
  const [maxPrice, setMaxPrice] = useState(10000);
  const [minRating, setMinRating] = useState(0);
  const [selectedAmenities, setSelectedAmenities] = useState([]);
  const [availableOnly, setAvailableOnly] = useState(false);

  const nights = calculateNights(checkInParam, checkOutParam);

  // Fetch hotels matching location & dates
  useEffect(() => {
    let isMounted = true;
    async function fetchResults() {
      try {
        setLoading(true);
        setError('');
        const data = await getHotels(locationParam, checkInParam, checkOutParam);

        if (!isMounted) return;
        setHotels(data);

        // Fetch availability for each hotel for selected dates
        const availMap = {};
        await Promise.all(
          data.map(async (h) => {
            try {
              const res = await checkAvailability(h.id, checkInParam, checkOutParam);
              availMap[h.id] = res;
            } catch {
              // If availability check fails, mark fallback
              availMap[h.id] = { available: true, availableRooms: '?' };
            }
          })
        );

        if (isMounted) {
          setAvailabilities(availMap);
        }
      } catch (err) {
        if (isMounted) {
          setError(err.message || 'Error loading search results.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchResults();
    return () => { isMounted = false; };
  }, [locationParam, checkInParam, checkOutParam]);

  // Extract all distinct amenities from loaded hotels for the filter checklist
  const allAmenities = useMemo(() => {
    const set = new Set();
    hotels.forEach((h) => {
      if (Array.isArray(h.amenities)) {
        h.amenities.forEach((a) => set.add(a));
      }
    });
    return Array.from(set).sort();
  }, [hotels]);

  const toggleAmenity = (amenity) => {
    setSelectedAmenities((prev) =>
      prev.includes(amenity) ? prev.filter((a) => a !== amenity) : [...prev, amenity]
    );
  };

  // Filter & Sort Logic
  const filteredAndSortedHotels = useMemo(() => {
    let list = [...hotels];

    // Filter by max price
    list = list.filter((h) => (h.pricePerNight ? Number(h.pricePerNight) <= maxPrice : true));

    // Filter by rating
    if (minRating > 0) {
      list = list.filter((h) => (h.rating ? Number(h.rating) >= minRating : false));
    }

    // Filter by amenities
    if (selectedAmenities.length > 0) {
      list = list.filter((h) => {
        if (!h.amenities) return false;
        return selectedAmenities.every((amenity) => h.amenities.includes(amenity));
      });
    }

    // Filter by availability
    if (availableOnly) {
      list = list.filter((h) => {
        const avail = availabilities[h.id];
        return avail ? avail.available : true;
      });
    }

    // Sort
    list.sort((a, b) => {
      const priceA = Number(a.pricePerNight || 0);
      const priceB = Number(b.pricePerNight || 0);
      const ratingA = Number(a.rating || 0);
      const ratingB = Number(b.rating || 0);

      if (sortBy === 'price-asc') return priceA - priceB;
      if (sortBy === 'price-desc') return priceB - priceA;
      if (sortBy === 'rating-desc') return ratingB - ratingA;
      return 0;
    });

    return list;
  }, [hotels, availabilities, maxPrice, minRating, selectedAmenities, availableOnly, sortBy]);

  const handleSearchUpdate = (newCriteria) => {
    const params = new URLSearchParams();
    if (newCriteria.location) params.append('location', newCriteria.location);
    params.append('checkIn', newCriteria.checkIn);
    params.append('checkOut', newCriteria.checkOut);
    setSearchParams(params);
  };

  return (
    <div className="search-results-page py-4">
      <div className="container">
        {/* Compact Search Bar Header */}
        <div className="mb-4">
          <SearchBar
            key={`${locationParam}-${checkInParam}-${checkOutParam}`}
            initialLocation={locationParam}
            initialCheckIn={checkInParam}
            initialCheckOut={checkOutParam}
            compact={true}
            onSearch={handleSearchUpdate}
          />
        </div>

        {/* Results Info & Sort Row */}
        <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 pb-2 border-bottom">
          <div>
            <h1 className="h4 fw-bold mb-1">
              {locationParam ? `Stays in "${locationParam}"` : 'All Available Stays'}
            </h1>
            <p className="text-muted small mb-0">
              {checkInParam} to {checkOutParam} ({nights} {nights === 1 ? 'night' : 'nights'}) &bull; Showing {filteredAndSortedHotels.length} of {hotels.length} stays
            </p>
          </div>

          <div className="d-flex align-items-center gap-2 mt-2 mt-md-0">
            <label htmlFor="sortSelect" className="small fw-semibold text-muted mb-0">
              Sort by:
            </label>
            <select
              id="sortSelect"
              className="form-select form-select-sm"
              style={{ width: 'auto' }}
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
            >
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="rating-desc">Rating: High to Low</option>
            </select>
          </div>
        </div>

        {/* Main Content: Left Filters & Right Cards */}
        <div className="row g-4">
          {/* Filter Sidebar */}
          <aside className="col-lg-3 col-md-4">
            <div className="card border p-3 bg-white rounded shadow-sm">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h6 className="fw-bold mb-0">Filter Stays</h6>
                {(maxPrice < 10000 || minRating > 0 || selectedAmenities.length > 0 || availableOnly) && (
                  <button
                    className="btn btn-sm btn-link text-decoration-none p-0 text-primary"
                    onClick={() => {
                      setMaxPrice(10000);
                      setMinRating(0);
                      setSelectedAmenities([]);
                      setAvailableOnly(false);
                    }}
                  >
                    Reset All
                  </button>
                )}
              </div>

              {/* Availability Filter */}
              <div className="mb-3 pb-3 border-bottom">
                <label className="fw-semibold small text-muted d-block mb-2">Availability</label>
                <div className="form-check">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    id="availCheck"
                    checked={availableOnly}
                    onChange={(e) => setAvailableOnly(e.target.checked)}
                  />
                  <label className="form-check-label small" htmlFor="availCheck">
                    Show Available Only
                  </label>
                </div>
              </div>

              {/* Price Range Filter */}
              <div className="mb-3 pb-3 border-bottom">
                <div className="d-flex justify-content-between align-items-center mb-1">
                  <label htmlFor="priceRange" className="fw-semibold small text-muted">Max Price / Night</label>
                  <span className="fw-bold text-dark small">₹{maxPrice.toLocaleString()}</span>
                </div>
                <input
                  id="priceRange"
                  type="range"
                  className="form-range"
                  min="3000"
                  max="10000"
                  step="500"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(Number(e.target.value))}
                />
                <div className="d-flex justify-content-between text-muted" style={{ fontSize: '0.75rem' }}>
                  <span>₹3,000</span>
                  <span>₹10,000</span>
                </div>
              </div>

              {/* Rating Filter */}
              <div className="mb-3 pb-3 border-bottom">
                <label className="fw-semibold small text-muted d-block mb-2">Guest Rating</label>
                <div className="d-flex flex-wrap gap-1">
                  {[
                    { label: 'All', value: 0 },
                    { label: '4.5+ ★', value: 4.5 },
                    { label: '4.7+ ★', value: 4.7 },
                    { label: '4.8+ ★', value: 4.8 },
                  ].map((r) => (
                    <button
                      key={r.label}
                      type="button"
                      className={`btn btn-sm ${minRating === r.value ? 'btn-primary' : 'btn-outline-secondary'}`}
                      style={{ fontSize: '0.8rem' }}
                      onClick={() => setMinRating(r.value)}
                    >
                      {r.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Amenities Checklist */}
              {allAmenities.length > 0 && (
                <div>
                  <label className="fw-semibold small text-muted d-block mb-2">Amenities</label>
                  <div style={{ maxHeight: '220px', overflowY: 'auto' }}>
                    {allAmenities.map((amenity) => (
                      <div key={amenity} className="form-check mb-1">
                        <input
                          type="checkbox"
                          className="form-check-input"
                          id={`amenity-${amenity}`}
                          checked={selectedAmenities.includes(amenity)}
                          onChange={() => toggleAmenity(amenity)}
                        />
                        <label className="form-check-label small" htmlFor={`amenity-${amenity}`}>
                          {amenity}
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </aside>

          {/* Hotel Cards List */}
          <main className="col-lg-9 col-md-8">
            {loading ? (
              <div className="text-center py-5 bg-white rounded border">
                <div className="spinner-border text-primary" role="status">
                  <span className="visually-hidden">Loading stays...</span>
                </div>
                <p className="text-muted mt-2 small">Searching and checking date availability...</p>
              </div>
            ) : error ? (
              <div className="alert alert-danger py-4 text-center">
                <h5>Unable to load stays</h5>
                <p className="mb-0">{error}</p>
              </div>
            ) : filteredAndSortedHotels.length === 0 ? (
              <div className="text-center py-5 bg-white rounded border p-4">
                <div className="fs-1 mb-2">🔍</div>
                <h5 className="fw-bold">No Matching Stays Found</h5>
                <p className="text-muted mb-3" style={{ maxWidth: '420px', margin: '0 auto' }}>
                  {locationParam
                    ? `No hotels matched "${locationParam}" with your current filter criteria. Try adjusting your destination, dates or filters.`
                    : 'No hotels match the selected filter parameters. Try loosening the price or rating filters.'}
                </p>
                <button
                  type="button"
                  className="btn btn-tt-outline btn-sm"
                  onClick={() => {
                    setMaxPrice(10000);
                    setMinRating(0);
                    setSelectedAmenities([]);
                    setAvailableOnly(false);
                    const params = new URLSearchParams();
                    params.append('checkIn', checkInParam);
                    params.append('checkOut', checkOutParam);
                    setSearchParams(params);
                  }}
                >
                  Clear Filters & Show All
                </button>
              </div>
            ) : (
              <div>
                {filteredAndSortedHotels.map((hotel) => (
                  <HotelCard
                    key={hotel.id}
                    hotel={hotel}
                    horizontal={true}
                    checkIn={checkInParam}
                    checkOut={checkOutParam}
                    availability={availabilities[hotel.id]}
                  />
                ))}
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
