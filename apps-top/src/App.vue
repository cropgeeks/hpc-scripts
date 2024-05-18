<template>
  <div id="app" v-if="jsonData">
    
    <b-container>
      <b-row>
        <b-col cols=12 md=4 lg=3><strong>System uptime:</strong></b-col>
        <b-col>{{ jsonData.system.uptime }}</b-col>
      </b-row>
      <b-row class="mb-2">
        <b-col cols=12 md=4 lg=3><strong>Power draw:</strong></b-col>
        <b-col>{{ (jsonData.system.watts / 1000).toFixed(2) }} kW</b-col>
      </b-row>
      <b-row>
        <b-col cols=12 md=4 lg=3><strong>Headnode CPU usage:</strong></b-col>
        <b-col>{{ jsonData.system.usage }}%</b-col>
      </b-row>
      <b-row>
        <b-col cols=12 md=4 lg=3><strong>Users:</strong></b-col>
        <b-col>{{ jsonData.system.users }} logged in; {{ jsonData.system.jobUsers }} with jobs running</b-col>
      </b-row>
      <b-row>
        <b-col cols=12 md=4 lg=3><strong>Active jobs:</strong></b-col>
        <b-col>{{ jsonData.system.jobCount }} (allocation efficiency {{ efficiency }}%)</b-col>
      </b-row>


      <div class="mt-4">
        <p>This page gives an overview of the current status of the cluster. While you can also <a target="_blank" href="https://ganglia.cropdiversity.ac.uk">view more detailed stats (per node)</a>,
        Slurm CPU allocation and power draw are only available here. Compare the darker and lighter coloured bars to get a quick overview of usage vs
        allocation - the bigger the difference, the less efficiently the cluster is being used.</p>
      </div>
      

      <div>
        <b-button size="sm" variant="link" v-b-toggle.charts>
          Show/hide charts
        </b-button>
        <b-collapse id="charts" class="mt-2">
          <div>
            <b-form-group>
              Plot data from the last:
              <b-form-radio-group size="sm" buttons v-model="graphIndex" :options="graphOptions"></b-form-radio-group>
            </b-form-group>
          </div>
          <b-link :href="usageGraphBig[graphIndex]" target="_blank">
            <b-img :src="usageGraph[graphIndex]" fluid class="mr-1 pt-1" />
          </b-link>
          <b-link :href="wattsGraphBig[graphIndex]" target="_blank">
            <b-img :src="wattsGraph[graphIndex]" fluid class="pt-1"/>
          </b-link>
        </b-collapse>
      </div>

      <hr>

      <b-form-checkbox v-model="hideOfflineNodes" :value="true" :unchecked-value="false" class="mb-2">
        Hide offline nodes
      </b-form-checkbox>

      <b-row>
        <b-col cols=12 md=4 lg=3>
          <b>HPC Node</b>
        </b-col>
        <b-col class="pr-0">
          <b>CPU Usage vs Allocated (%)</b>
        </b-col>
        <b-col>
          <b>Mem Usage vs Allocated (%)</b>
        </b-col>
      </b-row>

      <div class="mt-2 mb-3">
        <NodeStats v-bind:node="jsonData.nodesSummary" showCPUs="true"/>
      </div>

      <div v-for="node in jsonData.nodes" v-bind:key="node.name">
        <NodeStats v-bind:node="node" v-if="(node.online || !hideOfflineNodes)" showCPUs="true"/>
      </div>


      <hr>

      <b-row>
        <b-col cols=12 md=4 lg=3>
          <b>Storage Node</b>
        </b-col>
        <b-col class="pr-0">
          <b>Disk Usage (%)</b>
        </b-col>
        <b-col class="pr-0">
          <b>CPU Usage (%)</b>
        </b-col>
        <b-col>
          <b>Mem Usage (%)</b>
        </b-col>
      </b-row>

      <div class="mt-2 mb-3">
        <StorageStats v-bind:node="jsonData.beegfsSummary"/>
      </div>

      <div v-for="node in jsonData.beegfs" v-bind:key="node.name">
        <StorageStats v-bind:node="node" v-if="(node.online || !hideOfflineNodes)"/>
      </div>

      <hr>

      <div>
        <p>The cluster utilises a <a target="_blank" href="https://help.cropdiversity.ac.uk/green-computing.html">Green Computing</a> policy 
          that may keep idle HPC nodes powered off until they're needed.</p>
        <p>Green Computing metrics were last reset on {{ greenStart }}, and since then the policy has lowered energy consumption
          by <strong>{{ idleWatts }} kWh</strong> ({{ idleHours }} online node hours saved).</p>
      </div>

    </b-container>

  </div>
</template>

<script>
import axios from 'axios';
import NodeStats from './components/NodeStats.vue'
import StorageStats from './components/StorageStats.vue'

export default {
  name: 'App',
  components: {
    NodeStats,
    StorageStats
  },

  computed: {
    efficiency() {
      var alloc = this.jsonData.nodesSummary.alloc / this.jsonData.nodesSummary.cpus * 100;
      var usage = this.jsonData.nodesSummary.usage;

      if (alloc === 0)
        return "0.0";
      else
        return (usage / alloc * 100).toFixed(1);
    },
    idleHours() {
      return (this.jsonData.nodesSummary.minsOff / 60)
        .toLocaleString(undefined, {maximumFractionDigits: 1})
    },
    idleWatts() {
      return (this.jsonData.nodesSummary.idleWatts / 1000)
        .toLocaleString(undefined, {maximumFractionDigits: 1})
    },
    greenStart() {
      return new Date(this.jsonData.system.greenStart).toLocaleDateString()
    }
  },

  data() {
    return {
      jsonData: null,
      hideOfflineNodes: false,
      timer: '',
      usageGraph: [],
      usageGraphBig: [],
      wattsGraph: [],
      wattsGraphBig: [],
      graphIndex: 0,
      graphOptions: [
          { text: 'hour', value: 0 },
          { text: 'day', value: 1 },
          { text: 'week', value: 2 },
          { text: 'month', value: 3 },
          { text: 'year', value: 4 }
        ]
    }
  },

  created() {
    this.fetchJSON();
    this.timer = setInterval(this.fetchJSON, 30000);
  },

  methods: {
    async fetchJSON() {
      try {
        const res = await axios.get(`./stats.json`)
        this.jsonData = res.data;
      }
      catch(e) {
        console.error(e)
      }

      var labels = [ "1h", "1d", "1w", "1M", "1y" ];

      for (var i = 0; i < 5; i++)
      {
        this.usageGraph[i] = "./rrds/usagesmall_" + labels[i] + ".png?" + new Date().getTime();
        this.usageGraphBig[i] = "./rrds/usagelarge_" + labels[i] + ".png?" + new Date().getTime();
        this.wattsGraph[i] = "./rrds/wattssmall_" + labels[i] + ".png?" + new Date().getTime();
        this.wattsGraphBig[i] = "./rrds/wattslarge_" + labels[i] + ".png?" + new Date().getTime();
      }
    },
  },

  beforeDestroy() {
    clearInterval(this.timer)
  }
}

</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: left;
  color: #2c3e50;
  margin-top: 5px;
  margin-bottom: 5px;
  margin-left: 5px;
  margin-right: 5px;
  font-size: 0.85rem;
}
</style>
