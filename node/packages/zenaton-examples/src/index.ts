import { Client } from '@zenaton/client';
import { Worker } from '@zenaton/worker';
import { v4 as uuid } from 'uuid';

import { RefundBooking } from './tasks/refund-booking';

const opts = {
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
};

const client = new Client(opts);
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });

const worker = new Worker(opts);
worker.registerTask(new RefundBooking());
worker.run();