import { Link } from 'react-router-dom';
import { formatCurrency } from '../utils/dateUtils';

export default function HotelCard({
  hotel,
  horizontal = false,
  checkIn = '',
  checkOut = '',
  availability = null,
}) {
  const imageUrl = hotel.imageUrls && hotel.imageUrls.length > 0
    ? hotel.imageUrls[0]
    : 'https://images.unsplash.com/photo-1566073771259-6a8506099945';

  const detailsQuery = new URLSearchParams();
  if (checkIn) detailsQuery.append('checkIn', checkIn);
  if (checkOut) detailsQuery.append('checkOut', checkOut);
  const detailsUrl = `/hotels/${hotel.id}${detailsQuery.toString() ? `?${detailsQuery.toString()}` : ''}`;

  if (horizontal) {
    return (
      <div className="card tt-hotel-card-horizontal">
        <div className="row g-0">
          <div className="col-md-4 tt-card-img-wrap">
            <img
              src={imageUrl}
              className="tt-card-img-horizontal"
              alt={hotel.name}
              loading="lazy"
            />
            {availability && (
              <div className="position-absolute top-0 start-0 m-2">
                {availability.available ? (
                  <span className="tt-badge-available">
                    ● Available ({availability.availableRooms} left)
                  </span>
                ) : (
                  <span className="tt-badge-soldout">
                    ● Sold Out
                  </span>
                )}
              </div>
            )}
          </div>
          <div className="col-md-8 p-3 p-md-4 d-flex flex-column justify-content-between">
            <div>
              <div className="d-flex justify-content-between align-items-start gap-2 mb-1">
                <div>
                  <h5 className="card-title fw-bold mb-1">{hotel.name}</h5>
                  <p className="text-muted small mb-2">📍 {hotel.location}</p>
                </div>
                <div className="tt-rating-pill">
                  ★ {hotel.rating ? Number(hotel.rating).toFixed(1) : '4.5'}
                </div>
              </div>

              <p className="card-text text-muted small mb-2 line-clamp-2">
                {hotel.description}
              </p>

              <div className="mb-3">
                <span className="text-muted small fw-bold d-block mb-1">
                  Room: {hotel.roomName || 'Deluxe Room'} {hotel.capacity ? `(Sleeps ${hotel.capacity})` : ''}
                </span>
                <div className="d-flex flex-wrap gap-1">
                  {hotel.amenities && hotel.amenities.slice(0, 4).map((amenity, idx) => (
                    <span key={idx} className="tt-amenity-pill">{amenity}</span>
                  ))}
                  {hotel.amenities && hotel.amenities.length > 4 && (
                    <span className="tt-amenity-pill">+{hotel.amenities.length - 4} more</span>
                  )}
                </div>
              </div>
            </div>

            <div className="d-flex justify-content-between align-items-end pt-2 border-top">
              <div>
                <span className="text-muted small d-block">Price per night</span>
                <span className="h4 fw-bold text-dark mb-0">
                  {formatCurrency(hotel.pricePerNight)}
                </span>
              </div>
              <Link to={detailsUrl} className="btn btn-tt-primary">
                View Stay Details →
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Grid Card Mode (Home page Explore Stays)
  return (
    <div className="card tt-hotel-card">
      <div className="tt-card-img-wrap">
        <img
          src={imageUrl}
          className="card-img-top tt-card-img"
          alt={hotel.name}
          loading="lazy"
        />
        <div className="position-absolute top-0 end-0 m-2">
          <div className="tt-rating-pill">
            ★ {hotel.rating ? Number(hotel.rating).toFixed(1) : '4.5'}
          </div>
        </div>
      </div>
      <div className="card-body d-flex flex-column justify-content-between p-3">
        <div>
          <h6 className="card-title fw-bold mb-1">{hotel.name}</h6>
          <p className="text-muted small mb-2">📍 {hotel.location}</p>
          <p className="card-text text-muted small mb-3 text-truncate">
            {hotel.description}
          </p>
          <div className="mb-2">
            <span className="badge bg-light text-secondary border">
              {hotel.roomName || 'Deluxe Room'}
            </span>
          </div>
        </div>
        <div className="d-flex justify-content-between align-items-center pt-2 border-top">
          <div>
            <span className="text-muted small d-block" style={{ fontSize: '0.75rem' }}>From</span>
            <span className="fw-bold text-dark">
              {formatCurrency(hotel.pricePerNight)}
            </span>
            <span className="text-muted small"> / night</span>
          </div>
          <Link to={detailsUrl} className="btn btn-sm btn-tt-outline">
            Details
          </Link>
        </div>
      </div>
    </div>
  );
}
