import { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { createBooking } from '../services/api';
import { formatCurrency, formatDateDisplay } from '../utils/dateUtils';

export default function BookingDetails() {
  const location = useLocation();
  const navigate = useNavigate();

  // Booking selection context from HotelDetails
  const bookingState = location.state || {};

  const {
    hotelId,
    hotelName = 'Boutique Hotel',
    hotelLocation = 'India',
    roomTypeId,
    roomName = 'Deluxe Suite',
    pricePerNight = 0,
    checkIn = '',
    checkOut = '',
    nights = 1,
    estimatedTotal = 0,
  } = bookingState;

  // Form State
  const [customerName, setCustomerName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // If user navigated directly without selecting a hotel/room, prompt them
  if (!roomTypeId || !checkIn || !checkOut) {
    return (
      <div className="container py-5 text-center">
        <div className="card p-4 mx-auto border shadow-sm" style={{ maxWidth: '480px' }}>
          <h4 className="fw-bold mb-2">No Room Selected</h4>
          <p className="text-muted small mb-4">
            Please select a hotel stay and travel dates to start your reservation.
          </p>
          <Link to="/" className="btn btn-tt-primary">
            Explore Stays
          </Link>
        </div>
      </div>
    );
  }

  // Client-side quick validation before submission
  const validateForm = () => {
    const errors = {};
    if (!customerName.trim()) {
      errors.customerName = 'Full name is required.';
    } else if (customerName.length > 120) {
      errors.customerName = 'Full name must not exceed 120 characters.';
    }

    if (!email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Please provide a valid email address.';
    }

    if (!phone.trim()) {
      errors.phone = 'Phone number is required.';
    } else if (!/^[+]?[0-9\-\s()]{7,25}$/.test(phone.trim())) {
      errors.phone = 'Phone must be 7-25 characters and contain only digits, +, -, or spaces.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validateForm()) return;

    try {
      setIsSubmitting(true);

      const payload = {
        roomTypeId,
        customerName: customerName.trim(),
        email: email.trim().toLowerCase(),
        phone: phone.trim(),
        checkIn,
        checkOut,
      };

      // Backend call - authoritative price and lock applied in transaction
      const response = await createBooking(payload);

      // Only on confirmed backend success, navigate to the confirmation postcard
      navigate(`/confirmation/${encodeURIComponent(response.bookingReference)}`, {
        state: { booking: response },
        replace: true,
      });
    } catch (err) {
      if (err.fieldErrors && err.fieldErrors.length > 0) {
        setServerError(err.fieldErrors.join(', '));
      } else {
        setServerError(err.message || 'Unable to complete your booking. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="booking-details-page py-4">
      <div className="container">
        {/* Navigation Breadcrumb */}
        <nav aria-label="breadcrumb" className="mb-4">
          <ol className="breadcrumb small">
            <li className="breadcrumb-item"><Link to="/">Home</Link></li>
            <li className="breadcrumb-item"><Link to={`/hotels/${hotelId}`}>Stay Details</Link></li>
            <li className="breadcrumb-item active" aria-current="page">Guest Information & Confirmation</li>
          </ol>
        </nav>

        <h1 className="h3 fw-bold mb-4">Complete Your Reservation</h1>

        {serverError && (
          <div className="alert alert-danger py-3 px-4 mb-4 shadow-sm" role="alert">
            <div className="d-flex align-items-center gap-2">
              <span className="fs-5">⚠️</span>
              <div>
                <strong>Reservation Issue:</strong> {serverError}
              </div>
            </div>
          </div>
        )}

        <div className="row g-4">
          {/* Left Column: Guest Details Form */}
          <div className="col-lg-7">
            <div className="card p-4 border bg-white rounded shadow-sm">
              <h5 className="fw-bold mb-3 border-bottom pb-2">Guest Contact Details</h5>
              <p className="text-muted small mb-4">
                Your booking confirmation reference and destination postcard will be linked to this email address.
              </p>

              <form onSubmit={handleSubmit} noValidate>
                {/* Full Name */}
                <div className="mb-3">
                  <label htmlFor="customerName" className="form-label fw-semibold small text-muted">
                    Full Name <span className="text-danger">*</span>
                  </label>
                  <input
                    id="customerName"
                    type="text"
                    className={`form-control ${fieldErrors.customerName ? 'is-invalid' : ''}`}
                    placeholder="e.g. Priya Sharma"
                    value={customerName}
                    onChange={(e) => {
                      setCustomerName(e.target.value);
                      if (fieldErrors.customerName) setFieldErrors({ ...fieldErrors, customerName: null });
                    }}
                    maxLength={120}
                    required
                  />
                  {fieldErrors.customerName && (
                    <div className="invalid-feedback">{fieldErrors.customerName}</div>
                  )}
                </div>

                {/* Email Address */}
                <div className="mb-3">
                  <label htmlFor="customerEmail" className="form-label fw-semibold small text-muted">
                    Email Address <span className="text-danger">*</span>
                  </label>
                  <input
                    id="customerEmail"
                    type="email"
                    className={`form-control ${fieldErrors.email ? 'is-invalid' : ''}`}
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      if (fieldErrors.email) setFieldErrors({ ...fieldErrors, email: null });
                    }}
                    maxLength={254}
                    required
                  />
                  {fieldErrors.email && (
                    <div className="invalid-feedback">{fieldErrors.email}</div>
                  )}
                  <small className="form-text text-muted" style={{ fontSize: '0.78rem' }}>
                    Used along with your booking reference to retrieve or cancel your stay securely.
                  </small>
                </div>

                {/* Phone Number */}
                <div className="mb-4">
                  <label htmlFor="customerPhone" className="form-label fw-semibold small text-muted">
                    Contact Phone Number <span className="text-danger">*</span>
                  </label>
                  <input
                    id="customerPhone"
                    type="tel"
                    className={`form-control ${fieldErrors.phone ? 'is-invalid' : ''}`}
                    placeholder="+91 98765 43210"
                    value={phone}
                    onChange={(e) => {
                      setPhone(e.target.value);
                      if (fieldErrors.phone) setFieldErrors({ ...fieldErrors, phone: null });
                    }}
                    maxLength={25}
                    required
                  />
                  {fieldErrors.phone && (
                    <div className="invalid-feedback">{fieldErrors.phone}</div>
                  )}
                </div>

                <div className="p-3 bg-light rounded border mb-4">
                  <div className="d-flex align-items-center gap-2 mb-1">
                    <span className="text-primary fs-5">✓</span>
                    <strong className="small">Pessimistic Inventory Guarantee</strong>
                  </div>
                  <p className="text-muted small mb-0">
                    When you click confirm, the backend locks the room inventory row and verifies confirmed availability before completing your reservation.
                  </p>
                </div>

                {/* Submit Action */}
                <button
                  type="submit"
                  className="btn btn-tt-primary w-100 py-3 fw-bold fs-6"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Confirming Room & Generating Postcard...
                    </>
                  ) : (
                    `Confirm Reservation • ${formatCurrency(estimatedTotal)}`
                  )}
                </button>

                <p className="text-center text-muted small mt-2 mb-0" style={{ fontSize: '0.75rem' }}>
                  By confirming, you agree to the hotel cancellation policy.
                </p>
              </form>
            </div>
          </div>

          {/* Right Column: Authoritative Booking Summary */}
          <div className="col-lg-5">
            <div className="card border p-4 bg-white rounded shadow-sm sticky-top" style={{ top: '80px' }}>
              <h5 className="fw-bold mb-3 border-bottom pb-2">Stay Summary</h5>

              <div className="mb-3">
                <h6 className="fw-bold text-dark mb-1">{hotelName}</h6>
                <p className="text-muted small mb-2">📍 {hotelLocation}</p>
                <span className="badge bg-light text-dark border">
                  Room: {roomName}
                </span>
              </div>

              <div className="p-3 bg-light rounded border mb-3">
                <div className="row g-2 text-center">
                  <div className="col-6 border-end">
                    <span className="text-muted small d-block" style={{ fontSize: '0.75rem' }}>CHECK-IN</span>
                    <span className="fw-bold text-dark small">{formatDateDisplay(checkIn)}</span>
                  </div>
                  <div className="col-6">
                    <span className="text-muted small d-block" style={{ fontSize: '0.75rem' }}>CHECK-OUT</span>
                    <span className="fw-bold text-dark small">{formatDateDisplay(checkOut)}</span>
                  </div>
                </div>
              </div>

              <div className="mb-3">
                <div className="d-flex justify-content-between text-muted small mb-2">
                  <span>Stay Length</span>
                  <span className="fw-semibold text-dark">{nights} {nights === 1 ? 'Night' : 'Nights'}</span>
                </div>
                <div className="d-flex justify-content-between text-muted small mb-2">
                  <span>{formatCurrency(pricePerNight)} &times; {nights} {nights === 1 ? 'night' : 'nights'}</span>
                  <span>{formatCurrency(pricePerNight * nights)}</span>
                </div>
                <div className="d-flex justify-content-between text-muted small mb-2">
                  <span>Taxes & Fees</span>
                  <span className="text-success fw-semibold">Included</span>
                </div>
                <div className="d-flex justify-content-between fw-bold pt-2 border-top text-dark fs-5">
                  <span>Estimated Total</span>
                  <span className="text-primary">{formatCurrency(estimatedTotal)}</span>
                </div>
              </div>

              <div className="alert alert-info py-2 px-3 small mb-0" style={{ fontSize: '0.75rem' }}>
                ℹ️ <strong>Server-Authoritative Pricing:</strong> The final total is computed directly on the backend using database rates at transaction commit.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
