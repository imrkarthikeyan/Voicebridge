import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AudienceJoinPage } from '../AudienceJoinPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../api/meetings', () => ({
  getMeetingJoinInfo: vi.fn().mockResolvedValue({
    meetingCode: 'ABC123',
    title: 'Test Session',
    organizerName: 'Organizer Jane',
    status: 'ACTIVE',
  }),
}));

vi.mock('../../api/participants', () => ({
  joinMeeting: vi.fn().mockResolvedValue({
    sessionToken: 'mock-session-token',
    name: 'Alice',
    meetingCode: 'ABC123',
  }),
}));

describe('AudienceJoinPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders participant name input and join button after meeting info loads', async () => {
    render(
      <MemoryRouter initialEntries={['/join/ABC123']}>
        <Routes>
          <Route path="/join/:meetingCode" element={<AudienceJoinPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Test Session/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/John Doe/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Join Session/i })).toBeInTheDocument();
  });

  it('allows user to type name input and submit', async () => {
    render(
      <MemoryRouter initialEntries={['/join/ABC123']}>
        <Routes>
          <Route path="/join/:meetingCode" element={<AudienceJoinPage />} />
        </Routes>
      </MemoryRouter>
    );

    const nameInput = await screen.findByPlaceholderText(/John Doe/i);
    fireEvent.change(nameInput, { target: { value: 'Alice' } });

    expect(nameInput.value).toBe('Alice');
  });
});
