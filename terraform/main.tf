# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

resource "google_bigtable_instance" "instance" {
  name = "reporting-api-instance"

  cluster {
    cluster_id   = "reporting-api-instance-cluster"
    zone         = var.zone
    num_nodes    = 3
    storage_type = "HDD"
  }

  lifecycle {
    prevent_destroy = false
  }
}

resource "google_bigtable_table" "table" {
  name          = "security_report"
  instance_name = google_bigtable_instance.instance.name

  column_family {
    family = "description"
  }
}

resource "google_cloud_run_v2_service" "processor" {
  provider = "google"
  name     = "processor"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    containers {
      image = format("%s/forwarder", var.registry_path)

      env {
        name  = "BT_PROJECT"
        value = var.project
      }
      env {
        name  = "BT_INSTANCE"
        value = google_bigtable_instance.instance.name
      }
    }
  }
}
