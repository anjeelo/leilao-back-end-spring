-- src/main/resources/db/migration/V1__init.sql

-- Tabela de usuários
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name VARCHAR(150) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP WITH TIME ZONE
);

-- Tabela de leilões
CREATE TABLE auctions (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          title VARCHAR(200) NOT NULL,
                          description TEXT,
                          initial_price DECIMAL(12, 2) NOT NULL CHECK (initial_price > 0),
                          current_price DECIMAL(12, 2) NOT NULL,
                          start_date TIMESTAMP WITH TIME ZONE NOT NULL,
                          end_date TIMESTAMP WITH TIME ZONE NOT NULL CHECK (end_date > start_date),
                          status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                          seller_id UUID NOT NULL,
                          winner_id UUID,
                          image_filename VARCHAR(255),
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE,
                          CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id),
                          CONSTRAINT fk_auctions_winner FOREIGN KEY (winner_id) REFERENCES users(id)
);

-- Tabela de lances
CREATE TABLE bids (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
                      bidder_id UUID NOT NULL,
                      auction_id UUID NOT NULL,
                      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                      CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id),
                      CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id)
);

-- Índices para performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_seller ON auctions(seller_id);
CREATE INDEX idx_auctions_dates ON auctions(start_date, end_date);
CREATE INDEX idx_bids_auction ON bids(auction_id);
CREATE INDEX idx_bids_bidder ON bids(bidder_id);
CREATE INDEX idx_bids_auction_amount ON bids(auction_id, amount DESC);