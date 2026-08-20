import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { MeetingControlSidebar } from '../MeetingControlSidebar';

describe('MeetingControlSidebar Component', () => {
  const mockMeeting = {
    meetingCode: 'SIDE12',
    joinUrl: 'http://localhost/join/SIDE12',
  };

  const mockParticipants = [
    { id: 1, name: 'Alice' },
    { id: 2, name: 'Bob' },
  ];

  const mockQueue = [
    { id: 10, participantName: 'Alice', status: 'WAITING', requestedAt: new Date().toISOString() },
  ];

  const mockOnApprove = vi.fn();
  const mockOnReject = vi.fn();
  const mockOnEndSpeaker = vi.fn();
  const mockOnReorder = vi.fn();
  const mockOnOpenQrModal = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders meeting code and QR code section', () => {
    render(
      <MeetingControlSidebar
        meeting={mockMeeting}
        participants={mockParticipants}
        queue={mockQueue}
        onApprove={mockOnApprove}
        onReject={mockOnReject}
        onEndSpeaker={mockOnEndSpeaker}
        onReorder={mockOnReorder}
        onOpenQrModal={mockOnOpenQrModal}
      />
    );

    expect(screen.getByText('SIDE12')).toBeInTheDocument();
    expect(screen.getByText(/Current Speaker/i)).toBeInTheDocument();
    expect(screen.getByText(/Speaking Queue/i)).toBeInTheDocument();
    expect(screen.getByText(/Joined Audience/i)).toBeInTheDocument();
    expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
  });

  it('triggers approve callback when approve button is clicked', () => {
    render(
      <MeetingControlSidebar
        meeting={mockMeeting}
        participants={mockParticipants}
        queue={mockQueue}
        onApprove={mockOnApprove}
        onReject={mockOnReject}
        onEndSpeaker={mockOnEndSpeaker}
        onReorder={mockOnReorder}
        onOpenQrModal={mockOnOpenQrModal}
      />
    );

    const approveBtn = screen.getByRole('button', { name: /Approve/i });
    fireEvent.click(approveBtn);

    expect(mockOnApprove).toHaveBeenCalledWith(10);
  });
});
