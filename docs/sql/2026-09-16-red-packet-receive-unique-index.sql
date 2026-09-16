-- Before applying this constraint, manually clean up any duplicate packet/receiver rows.
ALTER TABLE red_packet_receive
  ADD CONSTRAINT uk_red_packet_receive_packet_receiver
  UNIQUE (red_packet_id, receiver_id);
