<template>
  <ms-container>
    <ms-main-container>
      <el-card v-loading="result.loading">
        <el-row>
          <el-col :span="16">
            <el-row>
              <el-breadcrumb separator-class="el-icon-arrow-right">
                <el-breadcrumb-item :to="{ path: '/performance/test/' + this.projectId }">{{ projectName }}
                </el-breadcrumb-item>
                <el-breadcrumb-item :to="{ path: '/performance/test/edit/' + this.testId }">{{ testName }}
                </el-breadcrumb-item>
                <el-breadcrumb-item>{{ reportName }}</el-breadcrumb-item>
              </el-breadcrumb>
            </el-row>
            <el-row class="ms-report-view-btns">
              <el-button :disabled="isReadOnly || report.status !== 'Running'" type="primary" plain size="mini"
                         @click="dialogFormVisible=true">
                {{ $t('report.test_stop_now') }}
              </el-button>
              <el-button :disabled="isReadOnly || report.status !== 'Completed'" type="success" plain size="mini"
                         @click="rerun(testId)">
                {{ $t('report.test_execute_again') }}
              </el-button>
              <el-button :disabled="isReadOnly" type="info" plain size="mini" @click="exportReport(reportName)">
                 {{$t('report.export')}}
              </el-button>

              <!--<el-button :disabled="isReadOnly" type="warning" plain size="mini">-->
                <!--{{$t('report.compare')}}-->
              <!--</el-button>-->
            </el-row>
          </el-col>
          <el-col :span="8">
            <span class="ms-report-time-desc">
              {{ $t('report.test_duration', [this.minutes, this.seconds]) }}
            </span>
            <span class="ms-report-time-desc">
              {{ $t('report.test_start_time') }}：{{ startTime }}
            </span>
            <span class="ms-report-time-desc">
              {{ $t('report.test_end_time') }}：{{ endTime }}
            </span>
          </el-col>
        </el-row>

        <el-divider/>
        <div ref="resume">
          <el-tabs v-model="active" type="border-card" :stretch="true">
            <el-tab-pane :label="$t('load_test.pressure_config')">
              <ms-performance-pressure-config :is-read-only="true" :report="report"/>
            </el-tab-pane>
            <el-tab-pane :label="$t('report.test_overview')">
              <ms-report-test-overview :report="report" ref="testOverview"/>
            </el-tab-pane>
            <el-tab-pane :label="$t('report.test_request_statistics')">
              <ms-report-request-statistics :report="report" ref="requestStatistics"/>
            </el-tab-pane>
            <el-tab-pane :label="$t('report.test_error_log')">
              <ms-report-error-log :report="report" ref="errorLog"/>
            </el-tab-pane>
            <el-tab-pane :label="$t('report.test_log_details')">
              <ms-report-log-details :report="report"/>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="report-export" v-show="reportExportVisible">
        <!--<div class="report-export">-->
          <el-card id="testOverview">
            <template v-slot:header >
              <slot name="header">
                <span class="title">{{$t('report.test_overview')}}</span>
              </slot>
            </template>
            <ms-report-test-overview :report="report" ref="testOverview"/>
          </el-card>
          <el-card id="requestStatistics" title="'requestStatistics'">
            <template v-slot:header >
              <slot name="header">
                <span class="title">{{$t('report.test_request_statistics')}}</span>
              </slot>
            </template>
            <ms-report-request-statistics :report="report" ref="requestStatistics"/>
          </el-card>
          <el-card id="errorLog" title="'errorLog'">
            <template v-slot:header >
              <slot name="header">
                <span class="title">{{$t('report.test_error_log')}}</span>
              </slot>
            </template>
            <ms-report-error-log :report="report" ref="errorLog"/>
          </el-card>
        </div>

      </el-card>
      <el-dialog :title="$t('report.test_stop_now_confirm')" :visible.sync="dialogFormVisible" width="30%">
        <p v-html="$t('report.force_stop_tips')"/>
        <p v-html="$t('report.stop_tips')"/>
        <div slot="footer" class="dialog-footer">
          <el-button type="danger" size="small" @click="stopTest(true)">{{ $t('report.force_stop_btn') }}
          </el-button>
          <el-button type="primary" size="small" @click="stopTest(false)">{{ $t('report.stop_btn') }}
          </el-button>
        </div>
      </el-dialog>
    </ms-main-container>
  </ms-container>
</template>

<script>
import MsReportErrorLog from './components/ErrorLog';
import MsReportLogDetails from './components/LogDetails';
import MsReportRequestStatistics from './components/RequestStatistics';
import MsReportTestOverview from './components/TestOverview';
import MsPerformancePressureConfig from "./components/PerformancePressureConfig";
import MsContainer from "../../common/components/MsContainer";
import MsMainContainer from "../../common/components/MsMainContainer";

import {checkoutTestManagerOrTestUser} from "@/common/js/utils";
import {exportPdf} from "../../../../common/js/utils";
import html2canvas from 'html2canvas';


export default {
  name: "PerformanceReportView",
  components: {
    MsReportErrorLog,
    MsReportLogDetails,
    MsReportRequestStatistics,
    MsReportTestOverview,
    MsContainer,
    MsMainContainer,
    MsPerformancePressureConfig
  },
  data() {
    return {
      result: {},
      active: '1',
      reportId: '',
      status: '',
      reportName: '',
      testId: '',
      testName: '',
      projectId: '',
      projectName: '',
      startTime: '0',
      endTime: '0',
      minutes: '0',
      seconds: '0',
      title: 'Logging',
      report: {},
      isReadOnly: false,
      websocket: null,
      dialogFormVisible: false,
      reportExportVisible: false,
      isShow: true,
      testPlan: {testResourcePoolId: null}
    }
  },
  methods: {
    initBreadcrumb(callback) {
      if (this.reportId) {
        this.result = this.$get("/performance/report/test/pro/info/" + this.reportId, res => {
          let data = res.data;
          if (data) {
            this.reportName = data.name;
            this.testId = data.testId;
            this.testName = data.testName;
            this.projectId = data.projectId;
            this.projectName = data.projectName;
            //
            if (callback) callback(res);
          } else {
            this.$error(this.$t('report.not_exist'));
          }
        })
      }
    },
    initReportTimeInfo() {
      if (this.status === 'Starting') {
        this.clearData();
        return;
      }
      if (this.reportId) {
        this.result = this.$get("/performance/report/content/report_time/" + this.reportId)
          .then(res => {
            let data = res.data.data;
            if (data) {
              this.startTime = data.startTime;
              this.endTime = data.endTime;
              let duration = data.duration;
              this.minutes = Math.floor(duration / 60);
              this.seconds = duration % 60;
            }
          }).catch(() => {
            this.clearData();
          });
      }
    },
    initWebSocket() {
      let protocol = "ws://";
      if (window.location.protocol === 'https:') {
        protocol = "wss://";
      }
      const uri = protocol + window.location.host + "/performance/report/" + this.reportId;
      this.websocket = new WebSocket(uri);
      this.websocket.onmessage = this.onMessage;
      this.websocket.onopen = this.onOpen;
      this.websocket.onerror = this.onError;
      this.websocket.onclose = this.onClose;
    },
    checkReportStatus(status) {
      switch (status) {
        case 'Error':
          this.$warning(this.$t('report.generation_error'));
          break;
        case 'Starting':
          this.$alert(this.$t('report.start_status'));
          break;
        case 'Reporting':
        case 'Running':
        case 'Completed':
        default:
          break;
      }
    },
    clearData() {
      this.startTime = '0';
      this.endTime = '0';
      this.minutes = '0';
      this.seconds = '0';
    },
    stopTest(forceStop) {
      this.result = this.$get('/performance/stop/' + this.reportId + '/' + forceStop, () => {
        this.$success(this.$t('report.test_stop_success'));
        if (forceStop) {
          this.$router.push('/performance/report/all');
        } else {
          this.report.status = 'Completed';
        }
      });
      this.dialogFormVisible = false;
    },
    rerun(testId) {
      this.$confirm(this.$t('report.test_rerun_confirm'), '', {
        confirmButtonText: this.$t('commons.confirm'),
        cancelButtonText: this.$t('commons.cancel'),
        type: 'warning'
      }).then(() => {
        this.result = this.$post('/performance/run', {id: testId, triggerMode: 'MANUAL'}, (response) => {
          this.reportId = response.data;
          this.$router.push({path: '/performance/report/view/' + this.reportId});
          // 注册 socket
          this.initWebSocket();
        })
      }).catch(() => {
      });
    },
    onOpen() {
      window.console.log("socket opening.");
    },
    onError(e) {
      window.console.error(e)
    },
    onMessage(e) {
      this.$set(this.report, "refresh", e.data); // 触发刷新
      this.$set(this.report, "status", 'Running');
      this.status = 'Running';
      this.initReportTimeInfo();
      window.console.log('receive a message:', e.data);
    },
    onClose(e) {
      if (e.code === 1005) {
        // 强制删除之后关闭socket，不用刷新report
        return;
      }
      this.$set(this.report, "refresh", Math.random()); // 触发刷新
      this.$set(this.report, "status", 'Completed');
      this.initReportTimeInfo();
      window.console.log("socket closed.");
    },
    exportReport(name) {
      this.result = {loading: true};
      let result = this.result;
      result.loading = true;
      this.reportExportVisible = true;
      let promises = [];
      let canvasList = new Array(3);
      let reset = this.exportReportReset;
      this.$nextTick(function () {
        setTimeout(() => {
          promises.push(this.getCanvasPromise('testOverview', 0, canvasList));
          promises.push(this.getCanvasPromise('requestStatistics', 1, canvasList));
          promises.push(this.getCanvasPromise('errorLog', 2, canvasList));

          Promise.all(promises).then(function (info) {
            exportPdf(name, canvasList);
            result.loading = false;
            reset();
          });
        }, 1000);
      })
    },
    exportReportReset() {
      this.reportExportVisible = false;
      this.isShow = true;
    },
    getCanvasPromise(id, index, canvasList) {
      return new Promise(function(resolve, reject) {
        html2canvas(document.getElementById(id), {
          scale: 2
        }).then(function(canvas) {
          canvasList[index] = canvas;
          resolve('success');
        });
      });
    },
  },
  created() {
    this.isReadOnly = false;
    if (!checkoutTestManagerOrTestUser()) {
      this.isReadOnly = true;
    }
    this.reportId = this.$route.path.split('/')[4];
    this.result = this.$get("/performance/report/" + this.reportId, res => {
      let data = res.data;
      if (data) {
        this.status = data.status;
        this.$set(this.report, "id", this.reportId);
        this.$set(this.report, "status", data.status);
        this.$set(this.report, "testId", data.testId);
        this.$set(this.report, "loadConfiguration", data.loadConfiguration);
        this.checkReportStatus(data.status);
        if (this.status === "Completed" || this.status === "Running") {
          this.initReportTimeInfo();
        }
        this.initBreadcrumb();
        this.initWebSocket();
      } else {
        this.$error(this.$t('report.not_exist'))
      }
    });

  },
  watch: {
    '$route'(to) {
      if (to.name === "perReportView") {
        this.isReadOnly = false;
        if (!checkoutTestManagerOrTestUser()) {
          this.isReadOnly = true;
        }
        let reportId = to.path.split('/')[4];
        this.reportId = reportId;
        this.initBreadcrumb((response) => {
          let data = response.data;

          this.$set(this.report, "id", reportId);
          this.$set(this.report, "status", data.status);

          this.checkReportStatus(data.status);
          this.initReportTimeInfo();
        });
        this.initWebSocket();
      } else {
        console.log("close socket.");
        this.websocket.close() //离开路由之后断开websocket连接
      }
    }
  }
}
</script>

<style scoped>

  .ms-report-view-btns {
    margin-top: 15px;
  }

  .ms-report-time-desc {
    text-align: left;
    display: block;
    color: #5C7878;
  }

  .report-export .el-card {
    margin-bottom: 15px;
  }

</style>
