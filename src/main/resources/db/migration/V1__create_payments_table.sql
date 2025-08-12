CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    correlation_id UUID NOT NULL UNIQUE,
    amount NUMERIC(10, 2) NOT NULL,
    processor VARCHAR(10) NOT NULL CHECK (processor IN ('default', 'fallback')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
