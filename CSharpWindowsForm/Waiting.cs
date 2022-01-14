using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace NES {
    public partial class Waiting : Form {
        Thread thread;
        public Waiting(ThreadStart task) {
            InitializeComponent();
            thread = new Thread(delegate () {
                task();
                this.BeginInvoke((MethodInvoker)delegate () {
                    this.Close();
                });
            });
            thread.IsBackground = true;
            thread.Start();
        }

        private void Waiting_FormClosing(object sender, FormClosingEventArgs e) {
            if (thread != null)
                thread.Abort();
        }
    }
}
